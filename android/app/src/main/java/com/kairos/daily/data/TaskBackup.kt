package com.kairos.daily.data

import android.content.Context
import com.kairos.daily.reminders.ReminderScheduler
import org.json.JSONArray
import org.json.JSONObject

class TaskBackup(private val context: Context) {
    private val repository = TaskRepository(context)
    private val reminders = ReminderScheduler(context)

    fun exportJson(): String {
        val root = JSONObject()
        root.put("format", "kairos-backup")
        root.put("version", 1)
        root.put("exportedAt", System.currentTimeMillis())
        val items = JSONArray()
        repository.getTasks().forEach { task ->
            items.put(JSONObject().apply {
                put("id", task.id)
                put("syncId", task.syncId)
                put("title", task.title)
                put("notes", task.notes)
                put("scheduledDate", task.scheduledDate ?: JSONObject.NULL)
                put("reminderMinutes", task.reminderMinutes ?: JSONObject.NULL)
                put("priority", task.priority)
                put("repeatRule", task.repeatRule)
                put("sortPosition", task.sortPosition)
                put("estimatedPomodoros", task.estimatedPomodoros)
                put("completedPomodoros", task.completedPomodoros)
                put("isCompleted", task.isCompleted)
                put("completedAt", task.completedAt ?: JSONObject.NULL)
                put("createdAt", task.createdAt)
                put("updatedAt", task.updatedAt)
            })
        }
        root.put("tasks", items)
        return root.toString(2)
    }

    fun restoreJson(json: String): Int {
        val root = JSONObject(json)
        require(root.optString("format") == "kairos-backup") { "Not a Kairos backup" }
        require(root.optInt("version") == 1) { "Unsupported backup version" }
        val items = root.getJSONArray("tasks")
        val restored = buildList {
            for (index in 0 until items.length()) {
                val value = items.getJSONObject(index)
                val title = value.getString("title").trim()
                require(title.isNotEmpty()) { "A task has no title" }
                add(Task(
                    id = value.optLong("id", 0),
                    syncId = value.optString("syncId").takeIf { it.isNotBlank() } ?: java.util.UUID.randomUUID().toString(),
                    title = title,
                    notes = value.optString("notes", ""),
                    scheduledDate = value.nullableString("scheduledDate"),
                    reminderMinutes = value.nullableInt("reminderMinutes"),
                    priority = value.optInt("priority", 0),
                    repeatRule = value.optString("repeatRule", RepeatRule.NONE.value),
                    sortPosition = value.optLong("sortPosition", value.optLong("createdAt", System.currentTimeMillis())),
                    estimatedPomodoros = value.optInt("estimatedPomodoros", 0),
                    completedPomodoros = value.optInt("completedPomodoros", 0),
                    isCompleted = value.optBoolean("isCompleted", false),
                    completedAt = value.nullableLong("completedAt"),
                    createdAt = value.optLong("createdAt", System.currentTimeMillis()),
                    updatedAt = value.optLong("updatedAt", System.currentTimeMillis())
                ))
            }
        }
        repository.getTasks().forEach { reminders.cancel(it.id) }
        repository.replaceAll(restored)
        if (PreferencesRepository(context).get().remindersOnThisDevice) repository.getTasks().forEach(reminders::schedule)
        return restored.size
    }

    private fun JSONObject.nullableString(key: String) = if (isNull(key) || !has(key)) null else getString(key)
    private fun JSONObject.nullableInt(key: String) = if (isNull(key) || !has(key)) null else getInt(key)
    private fun JSONObject.nullableLong(key: String) = if (isNull(key) || !has(key)) null else getLong(key)
}
