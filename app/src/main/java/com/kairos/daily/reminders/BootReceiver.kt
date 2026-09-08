package com.kairos.daily.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kairos.daily.data.TaskRepository
import com.kairos.daily.focus.FocusTimerRepository
import com.kairos.daily.data.PreferencesRepository

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val scheduler = ReminderScheduler(context)
        val remindersEnabled = PreferencesRepository(context).get().remindersOnThisDevice
        TaskRepository(context).getTasks().forEach { if (remindersEnabled) scheduler.schedule(it) else scheduler.cancel(it.id) }
        FocusTimerRepository(context).restoreAfterSystemChange()
    }
}
