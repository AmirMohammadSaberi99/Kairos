package com.kairos.daily.focus

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.kairos.daily.MainActivity
import com.kairos.daily.data.PreferencesRepository
import com.kairos.daily.data.TaskRepository

class FocusCompleteReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val timer = FocusTimerRepository(context)
        val state = timer.get()
        val taskId = state.taskId ?: return
        if (!state.running || state.endAt > System.currentTimeMillis() + 2_000) return

        val repository = TaskRepository(context)
        val task = repository.getTask(taskId) ?: run { timer.clear(); return }
        val preferences = PreferencesRepository(context).get()
        val completedBreak = state.isBreak

        if (completedBreak) {
            timer.clear()
        } else {
            repository.completePomodoro(taskId)
            timer.startBreak(taskId, preferences.breakMinutes)
        }

        createChannels(context)
        val open = PendingIntent.getActivity(
            context,
            taskId.hashCode(),
            Intent(context, MainActivity::class.java).putExtra(MainActivity.EXTRA_FOCUS_TASK_ID, taskId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, channelId(preferences.soundEnabled, preferences.vibrationEnabled))
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(if (completedBreak) "Break complete" else "Focus complete - break started")
            .setContentText(
                if (completedBreak) "Ready for the next focused session?"
                else "${task.title} - Rest for ${preferences.breakMinutes} minutes."
            )
            .setContentIntent(open)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .extend(NotificationCompat.WearableExtender().setDismissalId("focus-$taskId-${if (completedBreak) "break" else "focus"}"))
            .build()

        if (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(context).apply {
                cancel(FOCUS_NOTIFICATION_ID)
                notify(FOCUS_NOTIFICATION_ID, notification)
            }
        }
    }

    companion object {
        private const val FOCUS_NOTIFICATION_ID = 73_026

        fun channelId(sound: Boolean, vibration: Boolean) =
            "focus_timer_${if (sound) "bell" else "silent"}_${if (vibration) "vibrate" else "still"}_v2"

        fun createChannels(context: Context) {
            if (Build.VERSION.SDK_INT < 26) return
            val manager = context.getSystemService(NotificationManager::class.java)
            listOf(true to true, true to false, false to true, false to false).forEach { (sound, vibration) ->
                val channel = NotificationChannel(
                    channelId(sound, vibration),
                    "Focus timer alerts",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Bell and vibration when focus or break time ends"
                    enableVibration(vibration)
                    vibrationPattern = if (vibration) longArrayOf(0, 250, 120, 250, 120, 450) else null
                    if (sound) {
                        setSound(
                            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_ALARM)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build()
                        )
                    } else {
                        setSound(null, null)
                    }
                }
                manager.createNotificationChannel(channel)
            }
        }
    }
}
