package com.kairos.daily.ui

import com.kairos.daily.data.Task
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class PlanningFilterTest {
    private val today = LocalDate.of(2026, 8, 12)

    @Test
    fun todayContainsOnlyTasksDatedToday() {
        val tasks = listOf(
            task("today", "2026-08-12"),
            task("overdue", "2026-08-11"),
            task("future", "2026-08-13"),
            task("undated", null)
        )

        assertEquals(listOf("today"), tasksForToday(tasks, today).map { it.title })
    }

    @Test
    fun upcomingContainsOnlyFutureTasksInDateOrder() {
        val tasks = listOf(
            task("later", "2026-08-20", sort = 2),
            task("today", "2026-08-12"),
            task("tomorrow second", "2026-08-13", sort = 2),
            task("tomorrow first", "2026-08-13", sort = 1),
            task("invalid", "not-a-date")
        )

        assertEquals(
            listOf("tomorrow first", "tomorrow second", "later"),
            tasksForUpcoming(tasks, today).map { it.title }
        )
    }

    private fun task(title: String, date: String?, sort: Long = 0) =
        Task(title = title, scheduledDate = date, sortPosition = sort)
}
