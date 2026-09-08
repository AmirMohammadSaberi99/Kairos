package com.kairos.daily.data

import android.content.Context

data class UserPreferences(
    val defaultReminderMinutes: Int? = null,
    val weekStartsMonday: Boolean = true,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val quotesEnabled: Boolean = true,
    val notificationTone: String = NotificationTone.CALM.value,
    val followUpEnabled: Boolean = true,
    val focusMinutes: Int = 25,
    val breakMinutes: Int = 5,
    val remindersOnThisDevice: Boolean = true
)

class PreferencesRepository(context: Context) {
    private val app = context.applicationContext
    private val values = context.applicationContext.getSharedPreferences("kairos_preferences", Context.MODE_PRIVATE)
    private val defaultReminderOwner = app.resources.configuration.smallestScreenWidthDp < 600

    fun get() = UserPreferences(
        defaultReminderMinutes = values.getInt(KEY_DEFAULT_REMINDER, -1).takeIf { it >= 0 },
        weekStartsMonday = values.getBoolean(KEY_WEEK_START, true),
        soundEnabled = values.getBoolean(KEY_SOUND, true),
        vibrationEnabled = values.getBoolean(KEY_VIBRATION, true),
        quotesEnabled = values.getBoolean(KEY_QUOTES, true),
        notificationTone = values.getString(KEY_TONE, NotificationTone.CALM.value) ?: NotificationTone.CALM.value,
        followUpEnabled = values.getBoolean(KEY_FOLLOW_UP, true),
        focusMinutes = values.getInt(KEY_FOCUS_MINUTES, 25).coerceIn(1, 120),
        breakMinutes = values.getInt(KEY_BREAK_MINUTES, 5).coerceIn(1, 60),
        remindersOnThisDevice = values.getBoolean(KEY_REMINDER_OWNER, defaultReminderOwner)
    )

    fun setDefaultReminder(minutes: Int?) = values.edit().putInt(KEY_DEFAULT_REMINDER, minutes ?: -1).apply()
    fun setWeekStartsMonday(value: Boolean) = values.edit().putBoolean(KEY_WEEK_START, value).apply()
    fun setSound(value: Boolean) = values.edit().putBoolean(KEY_SOUND, value).apply()
    fun setVibration(value: Boolean) = values.edit().putBoolean(KEY_VIBRATION, value).apply()
    fun setQuotes(value: Boolean) = values.edit().putBoolean(KEY_QUOTES, value).apply()
    fun setNotificationTone(value: String) = values.edit().putString(KEY_TONE, NotificationTone.from(value).value).apply()
    fun setFollowUp(value: Boolean) = values.edit().putBoolean(KEY_FOLLOW_UP, value).apply()
    fun setFocusMinutes(value: Int) = values.edit().putInt(KEY_FOCUS_MINUTES, value.coerceIn(1, 120)).apply()
    fun setBreakMinutes(value: Int) = values.edit().putInt(KEY_BREAK_MINUTES, value.coerceIn(1, 60)).apply()
    fun setRemindersOnThisDevice(value: Boolean) = values.edit().putBoolean(KEY_REMINDER_OWNER, value).apply()

    companion object {
        private const val KEY_DEFAULT_REMINDER = "default_reminder"
        private const val KEY_WEEK_START = "week_starts_monday"
        private const val KEY_SOUND = "sound"
        private const val KEY_VIBRATION = "vibration"
        private const val KEY_QUOTES = "quotes"
        private const val KEY_TONE = "notification_tone"
        private const val KEY_FOLLOW_UP = "follow_up"
        private const val KEY_FOCUS_MINUTES = "focus_minutes"
        private const val KEY_BREAK_MINUTES = "break_minutes"
        private const val KEY_REMINDER_OWNER = "reminders_on_this_device"
    }
}
