package com.kairos.daily.sync

import android.content.Context
import android.os.Build
import com.kairos.daily.data.RepeatRule
import com.kairos.daily.data.Task
import com.kairos.daily.data.TaskRepository
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.security.SecureRandom
import java.util.concurrent.Executors

data class SyncResult(val received: Int, val deviceName: String)

class SyncSecurityGate(private val maxFailedAttempts: Int = 5, private val lockoutDurationMs: Long = 30_000L) {
    var failedAttempts = 0
        private set
    var lockoutUntil = 0L
        private set

    fun verifyCode(actual: String, provided: String, now: Long = System.currentTimeMillis()) {
        if (now < lockoutUntil) {
            val remainingSec = ((lockoutUntil - now) / 1000).coerceAtLeast(1)
            error("Too many failed attempts. Try again in $remainingSec seconds.")
        }
        if (provided != actual) {
            failedAttempts++
            if (failedAttempts >= maxFailedAttempts) {
                lockoutUntil = now + lockoutDurationMs
                failedAttempts = 0
            }
            error("Incorrect pairing code")
        }
        failedAttempts = 0
    }
}

class LocalSync(private val context: Context) {
    private val repository = TaskRepository(context.applicationContext)
    private var executor = Executors.newCachedThreadPool()
    private val settings = context.getSharedPreferences("kairos_sync", Context.MODE_PRIVATE)
    private val securityGate = SyncSecurityGate()
    @Volatile private var server: ServerSocket? = null

    private fun getOrCreateExecutor() = synchronized(this) {
        if (executor.isShutdown || executor.isTerminated) {
            executor = Executors.newCachedThreadPool()
        }
        executor
    }

    val pairingCode: String
        get() = settings.getString(KEY_CODE, null) ?: (SecureRandom().nextInt(900_000) + 100_000).toString().also {
            settings.edit().putString(KEY_CODE, it).apply()
        }

    val endpoint: String
        get() = "${localIpv4() ?: "Unavailable"}:$PORT"

    val lastStatus: String get() = settings.getString(KEY_STATUS, "Not connected") ?: "Not connected"

    fun start(onStatus: (String) -> Unit, onChanged: () -> Unit) {
        if (server != null) return
        getOrCreateExecutor().execute {
            try {
                val listener = ServerSocket(PORT).also { server = it }
                report("Ready at $endpoint · code $pairingCode", onStatus)
                while (!listener.isClosed) {
                    val socket = listener.accept()
                    getOrCreateExecutor().execute { handle(socket, onStatus, onChanged) }
                }
            } catch (error: Exception) {
                if (server != null) report("Sync receiver stopped: ${error.message}", onStatus)
            } finally {
                server = null
            }
        }
    }

    fun stop() {
        runCatching { server?.close() }
        server = null
        synchronized(this) {
            if (!executor.isShutdown) {
                runCatching { executor.shutdownNow() }
            }
        }
    }

    fun connect(endpoint: String, code: String, onResult: (Result<SyncResult>) -> Unit) {
        getOrCreateExecutor().execute {
            val result = runCatching {
                val parts = endpoint.trim().removePrefix("http://").split(":")
                require(parts.size == 2) { "Use IP:port, for example 192.168.0.98:$PORT" }
                Socket(parts[0], parts[1].toInt()).use { socket ->
                    socket.soTimeout = 15_000
                    val request = JSONObject()
                        .put("protocol", PROTOCOL)
                        .put("code", code.trim())
                        .put("device", deviceName())
                        .put("tasks", encodeTasks(repository.getSyncTasks()))
                    val writer = socket.getOutputStream().bufferedWriter()
                    writer.write(request.toString())
                    writer.newLine()
                    writer.flush()
                    val response = BufferedReader(InputStreamReader(socket.getInputStream())).readLine() ?: error("Peer sent no response")
                    val root = JSONObject(response)
                    if (!root.optBoolean("ok")) error(root.optString("error", "Sync rejected"))
                    val changed = repository.mergeSynced(decodeTasks(root.getJSONArray("tasks")))
                    SyncResult(changed, root.optString("device", "Kairos device"))
                }
            }
            onResult(result)
        }
    }

    private fun handle(socket: Socket, onStatus: (String) -> Unit, onChanged: () -> Unit) {
        socket.use {
            runCatching {
                it.soTimeout = 15_000
                val line = BufferedReader(InputStreamReader(it.getInputStream())).readLine() ?: error("Empty sync request")
                val request = JSONObject(line)
                require(request.optInt("protocol") == PROTOCOL) { "Incompatible Kairos sync version" }
                securityGate.verifyCode(actual = pairingCode, provided = request.optString("code"))
                val device = request.optString("device", "Kairos device")
                val changed = repository.mergeSynced(decodeTasks(request.getJSONArray("tasks")))
                val response = JSONObject()
                    .put("ok", true)
                    .put("device", deviceName())
                    .put("tasks", encodeTasks(repository.getSyncTasks()))
                it.getOutputStream().bufferedWriter().use { writer ->
                    writer.write(response.toString())
                    writer.newLine()
                    writer.flush()
                }
                if (changed > 0) onChanged()
                report("Synced with $device · $changed updates received", onStatus)
            }.onFailure { error ->
                runCatching {
                    it.getOutputStream().bufferedWriter().use { writer ->
                        writer.write(JSONObject().put("ok", false).put("error", error.message ?: "Sync failed").toString())
                        writer.newLine()
                    }
                }
                report("Sync attempt rejected: ${error.message}", onStatus)
            }
        }
    }

    private fun encodeTasks(tasks: List<Task>) = JSONArray().apply {
        tasks.forEach { task -> put(JSONObject().apply {
            put("syncId", task.syncId); put("title", task.title); put("notes", task.notes)
            put("scheduledDate", task.scheduledDate ?: JSONObject.NULL); put("reminderMinutes", task.reminderMinutes ?: JSONObject.NULL)
            put("priority", task.priority); put("repeatRule", task.repeatRule); put("sortPosition", task.sortPosition)
            put("estimatedPomodoros", task.estimatedPomodoros); put("completedPomodoros", task.completedPomodoros)
            put("isCompleted", task.isCompleted); put("completedAt", task.completedAt ?: JSONObject.NULL)
            put("createdAt", task.createdAt); put("updatedAt", task.updatedAt); put("isDeleted", task.isDeleted)
        }) }
    }

    private fun decodeTasks(items: JSONArray) = buildList {
        for (index in 0 until items.length()) {
            val value = items.getJSONObject(index)
            val syncId = value.getString("syncId")
            val title = value.getString("title").trim()
            require(syncId.isNotBlank() && title.isNotBlank()) { "Invalid task in sync payload" }
            add(Task(
                syncId = syncId, title = title, notes = value.optString("notes", ""),
                scheduledDate = value.stringOrNull("scheduledDate"), reminderMinutes = value.intOrNull("reminderMinutes"),
                priority = value.optInt("priority", 0), repeatRule = RepeatRule.from(value.optString("repeatRule")).value,
                sortPosition = value.optLong("sortPosition", value.optLong("createdAt")),
                estimatedPomodoros = value.optInt("estimatedPomodoros", 0), completedPomodoros = value.optInt("completedPomodoros", 0),
                isCompleted = value.optBoolean("isCompleted"), completedAt = value.longOrNull("completedAt"),
                createdAt = value.optLong("createdAt"), updatedAt = value.optLong("updatedAt"), isDeleted = value.optBoolean("isDeleted")
            ))
        }
    }

    private fun JSONObject.stringOrNull(key: String) = if (!has(key) || isNull(key)) null else getString(key)
    private fun JSONObject.intOrNull(key: String) = if (!has(key) || isNull(key)) null else getInt(key)
    private fun JSONObject.longOrNull(key: String) = if (!has(key) || isNull(key)) null else getLong(key)

    private fun localIpv4(): String? = NetworkInterface.getNetworkInterfaces().toList()
        .flatMap { it.inetAddresses.toList() }
        .firstOrNull { it is Inet4Address && !it.isLoopbackAddress && it.isSiteLocalAddress }
        ?.hostAddress

    private fun deviceName() = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}"

    private fun report(status: String, callback: (String) -> Unit) {
        settings.edit().putString(KEY_STATUS, status).apply()
        callback(status)
    }

    companion object {
        const val PORT = 45_873
        private const val PROTOCOL = 1
        private const val MAX_MESSAGE_CHARS = 10_000_000
        private const val KEY_CODE = "pairing_code"
        private const val KEY_STATUS = "last_status"
    }
}
