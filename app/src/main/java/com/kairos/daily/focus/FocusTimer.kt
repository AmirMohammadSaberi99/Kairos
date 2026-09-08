package com.kairos.daily.focus

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

data class FocusTimerState(
    val taskId: Long? = null,
    val running: Boolean = false,
    val endAt: Long = 0,
    val remainingMillis: Long = 0,
    val durationMinutes: Int = 25,
    val isBreak: Boolean = false
) {
    fun remaining(now: Long = System.currentTimeMillis()): Long =
        if (running) (endAt - now).coerceAtLeast(0) else remainingMillis.coerceAtLeast(0)
}

class FocusTimerRepository(context: Context) {
    private val app = context.applicationContext
    private val values = app.getSharedPreferences("kairos_focus", Context.MODE_PRIVATE)
    private val alarms = app.getSystemService(AlarmManager::class.java)

    fun get() = FocusTimerState(
        taskId = values.getLong(KEY_TASK_ID, -1).takeIf { it >= 0 },
        running = values.getBoolean(KEY_RUNNING, false),
        endAt = values.getLong(KEY_END_AT, 0),
        remainingMillis = values.getLong(KEY_REMAINING, 0),
        durationMinutes = values.getInt(KEY_DURATION, 25),
        isBreak = values.getBoolean(KEY_IS_BREAK, false)
    )

    fun start(taskId: Long, durationMinutes: Int) {
        val duration = durationMinutes.coerceIn(1, 120)
        val endAt = System.currentTimeMillis() + duration * 60_000L
        save(taskId, true, endAt, duration * 60_000L, duration, false)
        schedule(endAt)
    }

    fun startBreak(taskId: Long, durationMinutes: Int) {
        val duration = durationMinutes.coerceIn(1, 60)
        val endAt = System.currentTimeMillis() + duration * 60_000L
        save(taskId, true, endAt, duration * 60_000L, duration, true)
        schedule(endAt)
    }

    fun pause() {
        val state = get()
        val taskId = state.taskId ?: return
        val remaining = state.remaining()
        cancelAlarm()
        save(taskId, false, 0, remaining, state.durationMinutes, state.isBreak)
    }

    fun resume() {
        val state = get()
        val taskId = state.taskId ?: return
        val remaining = state.remainingMillis.coerceAtLeast(1_000)
        val endAt = System.currentTimeMillis() + remaining
        save(taskId, true, endAt, remaining, state.durationMinutes, state.isBreak)
        schedule(endAt)
    }

    fun clear() {
        cancelAlarm()
        values.edit().clear().commit()
    }

    fun restoreAfterSystemChange() {
        val state = get()
        if (!state.running || state.taskId == null) return
        if (state.endAt <= System.currentTimeMillis()) {
            FocusCompleteReceiver().onReceive(app, Intent(app, FocusCompleteReceiver::class.java))
        } else {
            schedule(state.endAt)
        }
    }

    private fun save(taskId: Long, running: Boolean, endAt: Long, remaining: Long, duration: Int, isBreak: Boolean) {
        values.edit()
            .putLong(KEY_TASK_ID, taskId)
            .putBoolean(KEY_RUNNING, running)
            .putLong(KEY_END_AT, endAt)
            .putLong(KEY_REMAINING, remaining)
            .putInt(KEY_DURATION, duration)
            .putBoolean(KEY_IS_BREAK, isBreak)
            .commit()
    }

    private fun schedule(triggerAt: Long) {
        val pending = alarmIntent()
        if (Build.VERSION.SDK_INT < 31 || alarms.canScheduleExactAlarms()) {
            alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        } else {
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        }
    }

    private fun cancelAlarm() = alarms.cancel(alarmIntent())

    private fun alarmIntent() = PendingIntent.getBroadcast(
        app, REQUEST_CODE, Intent(app, FocusCompleteReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    companion object {
        private const val REQUEST_CODE = 73_025
        private const val KEY_TASK_ID = "task_id"
        private const val KEY_RUNNING = "running"
        private const val KEY_END_AT = "end_at"
        private const val KEY_REMAINING = "remaining"
        private const val KEY_DURATION = "duration"
        private const val KEY_IS_BREAK = "is_break"
    }
}
