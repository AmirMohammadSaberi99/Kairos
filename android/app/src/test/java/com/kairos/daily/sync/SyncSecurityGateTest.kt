package com.kairos.daily.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class SyncSecurityGateTest {

    @Test
    fun correctCodeSucceedsAndResetsAttempts() {
        val gate = SyncSecurityGate()
        // 1 wrong attempt
        try {
            gate.verifyCode("123456", "000000", now = 1000L)
            fail("Expected exception on wrong code")
        } catch (e: IllegalStateException) {
            assertEquals("Incorrect pairing code", e.message)
            assertEquals(1, gate.failedAttempts)
        }

        // Correct code resets failed attempts
        gate.verifyCode("123456", "123456", now = 2000L)
        assertEquals(0, gate.failedAttempts)
        assertEquals(0L, gate.lockoutUntil)
    }

    @Test
    fun fiveFailedAttemptsTriggersLockout() {
        val gate = SyncSecurityGate(maxFailedAttempts = 5, lockoutDurationMs = 30_000L)
        val startTime = 10_000L

        for (attempt in 1..4) {
            try {
                gate.verifyCode("123456", "wrong", now = startTime)
                fail("Expected failure on attempt $attempt")
            } catch (e: IllegalStateException) {
                assertEquals("Incorrect pairing code", e.message)
                assertEquals(attempt, gate.failedAttempts)
            }
        }

        // 5th attempt triggers lockout
        try {
            gate.verifyCode("123456", "wrong", now = startTime)
            fail("Expected failure on 5th attempt")
        } catch (e: IllegalStateException) {
            assertEquals("Incorrect pairing code", e.message)
            assertEquals(0, gate.failedAttempts)
            assertEquals(startTime + 30_000L, gate.lockoutUntil)
        }

        // Attempt during lockout is immediately rejected
        try {
            gate.verifyCode("123456", "123456", now = startTime + 5_000L)
            fail("Expected lockout exception")
        } catch (e: IllegalStateException) {
            assertTrue(e.message?.contains("Too many failed attempts") == true)
        }

        // After lockout duration expires, correct code succeeds
        gate.verifyCode("123456", "123456", now = startTime + 31_000L)
        assertEquals(0, gate.failedAttempts)
    }
}
