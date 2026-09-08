package com.kairos.daily.data

data class Task(
    val id: Long = 0,
    val syncId: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val notes: String = "",
    val scheduledDate: String?,
    val reminderMinutes: Int? = null,
    val priority: Int = 0,
    val repeatRule: String = "NONE",
    val sortPosition: Long = System.currentTimeMillis(),
    val estimatedPomodoros: Int = 0,
    val completedPomodoros: Int = 0,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)

enum class EisenhowerQuadrant(val value: Int, val label: String, val hint: String) {
    NONE(0, "No box", "Unsorted"),
    DO_NOW(1, "Do now", "Urgent + important"),
    SCHEDULE(2, "Schedule", "Important, not urgent"),
    DELEGATE(3, "Delegate", "Urgent, not important"),
    ELIMINATE(4, "Eliminate", "Not urgent or important");

    companion object { fun from(value: Int) = entries.firstOrNull { it.value == value } ?: NONE }
}

enum class RepeatRule(val value: String, val label: String) {
    NONE("NONE", "Never"), DAILY("DAILY", "Daily"), WEEKDAYS("WEEKDAYS", "Weekdays"), WEEKLY("WEEKLY", "Weekly");
    companion object { fun from(value: String) = entries.firstOrNull { it.value == value } ?: NONE }
}

enum class NotificationTone(val value: String, val label: String) {
    CALM("CALM", "Calm"), FIRM("FIRM", "Firm"), STRICT("STRICT", "Strict");
    companion object { fun from(value: String) = entries.firstOrNull { it.value == value } ?: CALM }
}

fun Task.nextOccurrence(): Task? {
    val rule = RepeatRule.from(repeatRule)
    if (rule == RepeatRule.NONE) return null
    val current = scheduledDate?.let { runCatching { java.time.LocalDate.parse(it) }.getOrNull() } ?: java.time.LocalDate.now()
    val next = when (rule) {
        RepeatRule.DAILY -> current.plusDays(1)
        RepeatRule.WEEKLY -> current.plusWeeks(1)
        RepeatRule.WEEKDAYS -> generateSequence(current.plusDays(1)) { it.plusDays(1) }.first { it.dayOfWeek.value <= 5 }
        RepeatRule.NONE -> return null
    }
    val now = System.currentTimeMillis()
    return copy(
        id = 0,
        syncId = java.util.UUID.randomUUID().toString(),
        scheduledDate = next.toString(),
        isCompleted = false,
        completedAt = null,
        completedPomodoros = 0,
        createdAt = now,
        updatedAt = now,
        sortPosition = now
    )
}

fun reminderCopy(tone: NotificationTone, title: String, stage: Int): Pair<String, String> = when (tone) {
    NotificationTone.CALM -> if (stage == 0) title to "You planned this for now. Start with five focused minutes."
        else title to "Still open. Start a short focus session or reschedule it."
    NotificationTone.FIRM -> if (stage == 0) title to "This is the time you chose. Begin the first focused step."
        else title to "Still open. Start now or reschedule it honestly."
    NotificationTone.STRICT -> if (stage == 0) title to "You made this commitment. Give it one focused session."
        else title to "Ignoring it will not remove it. Start, reschedule, or delete it."
}
