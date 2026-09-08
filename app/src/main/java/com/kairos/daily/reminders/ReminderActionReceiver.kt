package com.kairos.daily.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.kairos.daily.data.TaskRepository
import com.kairos.daily.data.nextOccurrence
import com.kairos.daily.data.PreferencesRepository

class ReminderActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(ReminderScheduler.EXTRA_TASK_ID, -1)
        if (taskId < 0) return
        when (intent.action) {
            ACTION_COMPLETE -> {
                val repository = TaskRepository(context)
                repository.getTask(taskId)?.let { task ->
                    repository.update(task.copy(isCompleted = true, completedAt = System.currentTimeMillis()))
                    task.nextOccurrence()?.let { next ->
                        val nextId = repository.add(next)
                        if (PreferencesRepository(context).get().remindersOnThisDevice) {
                            ReminderScheduler(context).schedule(next.copy(id = nextId))
                        }
                    }
                }
                ReminderScheduler(context).cancel(taskId)
                NotificationManagerCompat.from(context).cancel(taskId.hashCode())
            }
            ACTION_SNOOZE -> {
                val scheduler = ReminderScheduler(context)
                scheduler.cancel(taskId)
                scheduler.scheduleAt(taskId, System.currentTimeMillis() + 10 * 60 * 1000)
                NotificationManagerCompat.from(context).cancel(taskId.hashCode())
            }
        }
    }

    companion object {
        const val ACTION_COMPLETE = "com.kairos.daily.COMPLETE"
        const val ACTION_SNOOZE = "com.kairos.daily.SNOOZE"
    }
}
