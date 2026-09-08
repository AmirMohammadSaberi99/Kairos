package com.kairos.daily.sync

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.kairos.daily.MainActivity
import com.kairos.daily.data.PreferencesRepository
import com.kairos.daily.data.TaskRepository
import com.kairos.daily.reminders.ReminderScheduler

class SyncReceiverService : Service() {
    private lateinit var sync: LocalSync

    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private val autoStopRunnable = Runnable {
        sync.stop()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onCreate() {
        super.onCreate()
        sync = LocalSync(this)
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            handler.removeCallbacks(autoStopRunnable)
            sync.stop()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        resetInactivityTimer()
        startForeground(NOTIFICATION_ID, notification("Ready at ${sync.endpoint} · code ${sync.pairingCode}"))
        sync.start(
            onStatus = { status -> getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification(status)) },
            onChanged = {
                resetInactivityTimer()
                reconcileReminders()
            }
        )
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(autoStopRunnable)
        sync.stop()
        super.onDestroy()
    }

    override fun onTimeout(startId: Int) {
        super.onTimeout(startId)
        handler.removeCallbacks(autoStopRunnable)
        sync.stop()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun resetInactivityTimer() {
        handler.removeCallbacks(autoStopRunnable)
        handler.postDelayed(autoStopRunnable, INACTIVITY_TIMEOUT_MS)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun reconcileReminders() {
        val scheduler = ReminderScheduler(this)
        val enabled = PreferencesRepository(this).get().remindersOnThisDevice
        TaskRepository(this).getSyncTasks().forEach { task ->
            if (enabled && !task.isDeleted) scheduler.schedule(task) else scheduler.cancel(task.id)
        }
    }

    private fun notification(status: String): android.app.Notification {
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val stop = PendingIntent.getService(this, 1, Intent(this, SyncReceiverService::class.java).setAction(ACTION_STOP), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle("Kairos local sync is available")
            .setContentText(status)
            .setContentIntent(open)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(0, "Stop", stop)
            .build()
    }

    private fun createChannel() {
        val channel = NotificationChannel(CHANNEL_ID, "Local device sync", NotificationManager.IMPORTANCE_LOW).apply {
            description = "Shows when this device is receiving Kairos data over private Wi-Fi"
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        const val ACTION_STOP = "com.kairos.daily.STOP_SYNC"
        private const val CHANNEL_ID = "local_device_sync"
        private const val NOTIFICATION_ID = 45_873
        private const val INACTIVITY_TIMEOUT_MS = 60 * 60 * 1000L // 60 minutes
    }
}
