package com.example.feelingslog

import org.junit.Test
import org.junit.Assert.*

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun feelingLog_softDeleteState_isCorrect() {
        val log = FeelingLog(title = "Test Log", blocks = listOf(LogContentBlock.Text("Content")))
        assertNull(log.deletedAt)

        val deletedTimestamp = System.currentTimeMillis()
        val deletedLog = log.copy(deletedAt = deletedTimestamp)
        assertNotNull(deletedLog.deletedAt)
        assertEquals(deletedTimestamp, deletedLog.deletedAt)

        val restoredLog = deletedLog.copy(deletedAt = null)
        assertNull(restoredLog.deletedAt)
        assertEquals(log.id, restoredLog.id)
        assertEquals(log.createdAt, restoredLog.createdAt)
    }

    @Test
    fun thirtyDayThreshold_calculation_isCorrect() {
        val now = System.currentTimeMillis()
        val thirtyDaysMs = 30 * 24 * 60 * 60 * 1000L
        val threshold = now - thirtyDaysMs

        val expiredTimestamp = now - (31 * 24 * 60 * 60 * 1000L)
        val activeDeletedTimestamp = now - (5 * 24 * 60 * 60 * 1000L)

        assertTrue(expiredTimestamp <= threshold)
        assertFalse(activeDeletedTimestamp <= threshold)
    }
}
