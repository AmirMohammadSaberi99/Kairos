package com.kairos.daily.focus

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusTimerStateTest {
    @Test
    fun breakPhaseIsExplicitAndCountsDown() {
        val state = FocusTimerState(
            taskId = 7,
            running = true,
            endAt = 400_000,
            remainingMillis = 300_000,
            durationMinutes = 5,
            isBreak = true
        )

        assertTrue(state.isBreak)
        assertEquals(300_000, state.remaining(100_000))
    }

    @Test
    fun newTimerDefaultsToFocusPhase() {
        assertFalse(FocusTimerState().isBreak)
    }
}
