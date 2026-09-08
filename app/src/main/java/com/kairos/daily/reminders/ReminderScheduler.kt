package com.kairos.daily.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.kairos.daily.data.Task
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.Instant

class ReminderScheduler(private val context: Context) {
    private val alarms = context.getSystemService(AlarmManager::class.java)

    fun schedule(task: Task) {
        cancel(task.id)
        if (task.isCompleted) return
        val date = task.scheduledDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return
        val minutes = task.reminderMinutes ?: return
        val triggerAt = date.atTime(LocalTime.of(minutes / 60, minutes % 60))
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        if (triggerAt > System.currentTimeMillis()) scheduleAt(task.id, triggerAt)
    }

    fun scheduleAt(taskId: Long, triggerAt: Long) {
        schedulePending(triggerAt, reminderIntent(taskId))
    }

    fun scheduleFollowUp(taskId: Long) {
        val triggerAt = System.currentTimeMillis() + 10 * 60_000L
        val hour = Instant.ofEpochMilli(triggerAt).atZone(ZoneId.systemDefault()).hour
        if (hour in 7 until 22) schedulePending(triggerAt, followUpIntent(taskId))
    }

    private fun schedulePending(triggerAt: Long, pending: PendingIntent) {
        if (Build.VERSION.SDK_INT < 31 || alarms.canScheduleExactAlarms()) {
            alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        } else {
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        }
    }

    fun cancel(taskId: Long) {
        alarms.cancel(reminderIntent(taskId))
        alarms.cancel(followUpIntent(taskId))
    }

    private fun reminderIntent(taskId: Long) = PendingIntent.getBroadcast(
        context, taskId.hashCode(),
        Intent(context, ReminderReceiver::class.java).putExtra(EXTRA_TASK_ID, taskId),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun followUpIntent(taskId: Long) = PendingIntent.getBroadcast(
        context, taskId.hashCode() + 30_000,
        Intent(context, ReminderReceiver::class.java)
            .putExtra(EXTRA_TASK_ID, taskId)
            .putExtra(ReminderReceiver.EXTRA_STAGE, 1),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    companion object { const val EXTRA_TASK_ID = "task_id" }
}
