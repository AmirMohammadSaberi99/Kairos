package com.kairos.daily.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskTest {

    @Test
    fun newerRemoteVersionWins() {
        val local = Task(syncId = "shared", title = "Old", scheduledDate = null, updatedAt = 100)
        val remote = local.copy(title = "New", updatedAt = 101)
        assertTrue(shouldApplyRemote(local, remote))
    }

    @Test
    fun olderRemoteVersionDoesNotOverwriteLocal() {
        val local = Task(syncId = "shared", title = "New", scheduledDate = null, updatedAt = 101)
        val remote = local.copy(title = "Old", updatedAt = 100)
        assertFalse(shouldApplyRemote(local, remote))
    }

    @Test
    fun newerTombstoneWins() {
        val local = Task(syncId = "shared", title = "Task", scheduledDate = null, updatedAt = 100)
        val deleted = local.copy(isDeleted = true, updatedAt = 101)
        assertTrue(shouldApplyRemote(local, deleted))
    }
    private fun task(date: String, repeat: RepeatRule) = Task(
        id = 42,
        title = "Test",
        scheduledDate = date,
        repeatRule = repeat.value,
        isCompleted = true,
        completedAt = 1L
    )

    @Test fun quadrantValuesRemainStableForDatabaseStorage() {
        assertEquals(EisenhowerQuadrant.DO_NOW, EisenhowerQuadrant.from(1))
        assertEquals(EisenhowerQuadrant.SCHEDULE, EisenhowerQuadrant.from(2))
        assertEquals(EisenhowerQuadrant.DELEGATE, EisenhowerQuadrant.from(3))
        assertEquals(EisenhowerQuadrant.ELIMINATE, EisenhowerQuadrant.from(4))
        assertEquals(EisenhowerQuadrant.NONE, EisenhowerQuadrant.from(99))
    }

    @Test fun dailyOccurrenceAdvancesOneDayAndResetsCompletion() {
        val original = task("2026-08-12", RepeatRule.DAILY)
        val next = original.nextOccurrence()!!
        assertEquals("2026-08-13", next.scheduledDate)
        assertEquals(0L, next.id)
        assertFalse(original.syncId == next.syncId)
        assertFalse(next.isCompleted)
        assertNull(next.completedAt)
    }

    @Test fun weekdayOccurrenceSkipsWeekend() {
        val next = task("2026-08-14", RepeatRule.WEEKDAYS).nextOccurrence()!!
        assertEquals("2026-08-17", next.scheduledDate)
    }

    @Test fun weeklyOccurrenceAdvancesSevenDays() {
        val next = task("2026-08-12", RepeatRule.WEEKLY).nextOccurrence()!!
        assertEquals("2026-08-19", next.scheduledDate)
    }

    @Test fun nonRepeatingTaskHasNoNextOccurrence() {
        assertNull(task("2026-08-12", RepeatRule.NONE).nextOccurrence())
    }

    @Test fun manualMoveSwapsOnlyTheTwoSortPositions() {
        val first = task("2026-08-12", RepeatRule.NONE).copy(id = 1, title = "First", sortPosition = 100)
        val second = task("2026-08-12", RepeatRule.NONE).copy(id = 2, title = "Second", sortPosition = 200)
        val swapped = swappedPositions(listOf(first, second), second.id, -1)!!
        assertEquals(100L, swapped.first.sortPosition)
        assertEquals(200L, swapped.second.sortPosition)
        assertEquals("Second", swapped.first.title)
        assertEquals("First", swapped.second.title)
    }

    @Test fun manualMoveRejectsEdgesAndInvalidDirections() {
        val only = task("2026-08-12", RepeatRule.NONE).copy(id = 1)
        assertNull(swappedPositions(listOf(only), only.id, -1))
        assertNull(swappedPositions(listOf(only), only.id, 2))
    }

    @Test fun recurringOccurrenceKeepsEstimateButResetsFocusProgress() {
        val next = task("2026-08-12", RepeatRule.DAILY)
            .copy(estimatedPomodoros = 4, completedPomodoros = 3)
            .nextOccurrence()!!
        assertEquals(4, next.estimatedPomodoros)
        assertEquals(0, next.completedPomodoros)
    }

    @Test fun notificationToneParsingFallsBackToCalm() {
        assertEquals(NotificationTone.STRICT, NotificationTone.from("STRICT"))
        assertEquals(NotificationTone.CALM, NotificationTone.from("unknown"))
    }

    @Test fun strictFollowUpIsFirmButNotShaming() {
        val copy = reminderCopy(NotificationTone.STRICT, "Write report", 1)
        assertEquals("Write report", copy.first)
        assertEquals("Ignoring it will not remove it. Start, reschedule, or delete it.", copy.second)
    }
}
