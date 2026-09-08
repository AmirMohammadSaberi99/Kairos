package com.kairos.daily.reminders

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.kairos.daily.MainActivity
import com.kairos.daily.data.NotificationTone
import com.kairos.daily.data.reminderCopy
import com.kairos.daily.data.TaskRepository
import com.kairos.daily.data.PreferencesRepository

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskIdExtra = intent.getLongExtra(ReminderScheduler.EXTRA_TASK_ID, -1)
        val syncId = intent.getStringExtra(EXTRA_TASK_SYNC_ID)
        val repository = TaskRepository(context)
        val task = (if (taskIdExtra >= 0) repository.getTask(taskIdExtra) else syncId?.let(repository::getTaskBySyncId)) ?: return
        val taskId = task.id
        if (task.isCompleted) return
        val stage = intent.getIntExtra(EXTRA_STAGE, 0)
        createChannels(context)
        val preferences = PreferencesRepository(context).get()
        val open = PendingIntent.getActivity(context, taskId.hashCode(), Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val focus = PendingIntent.getActivity(
            context, taskId.hashCode() + 40_000,
            Intent(context, MainActivity::class.java).putExtra(MainActivity.EXTRA_FOCUS_TASK_ID, taskId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val copy = reminderCopy(NotificationTone.from(preferences.notificationTone), task.title, stage)
        val notification = NotificationCompat.Builder(context, channelId(preferences.soundEnabled, preferences.vibrationEnabled))
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(copy.first)
            .setContentText(copy.second)
            .setContentIntent(open)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .addAction(0, "Start focus", focus)
            .addAction(0, "Complete", action(context, taskId, ReminderActionReceiver.ACTION_COMPLETE, 10_000))
            .addAction(0, "Snooze 10 min", action(context, taskId, ReminderActionReceiver.ACTION_SNOOZE, 20_000))
            .extend(NotificationCompat.WearableExtender().setDismissalId("task-$taskId"))
            .build()
        if (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(context).notify(taskId.hashCode(), notification)
        }
        if (stage == 0 && preferences.followUpEnabled) ReminderScheduler(context).scheduleFollowUp(taskId)
    }

    private fun action(context: Context, taskId: Long, name: String, offset: Int) = PendingIntent.getBroadcast(
        context, taskId.hashCode() + offset,
        Intent(context, ReminderActionReceiver::class.java).setAction(name).putExtra(ReminderScheduler.EXTRA_TASK_ID, taskId),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    companion object {
        fun channelId(sound: Boolean, vibration: Boolean) = "task_reminders_${if (sound) "sound" else "silent"}_${if (vibration) "vibrate" else "still"}"

        const val EXTRA_STAGE = "reminder_stage"
        const val EXTRA_TASK_SYNC_ID = "task_sync_id"

        fun createChannels(context: Context) {
            if (Build.VERSION.SDK_INT >= 26) {
                listOf(true to true, true to false, false to true, false to false).forEach { (sound, vibration) ->
                    val behavior = listOfNotNull(if (sound) "sound" else null, if (vibration) "vibration" else null).joinToString(" and ").ifEmpty { "silent" }
                    val channel = NotificationChannel(channelId(sound, vibration), "Task reminders · $behavior", NotificationManager.IMPORTANCE_HIGH).apply {
                        description = "Task reminders with $behavior"
                        enableVibration(vibration)
                        if (!sound) setSound(null, null)
                    }
                    context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
                }
            }
        }
    }
}
