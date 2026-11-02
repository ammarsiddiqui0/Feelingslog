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
        val log = FeelingLog(title = "Test Log")
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

    @Test
    fun feelingLogWithDetails_topLevelOrdering_isCorrect() {
        val log = FeelingLog(title = "Ordered Log")
        val standalone1 = LogContentItem(
            logId = log.id,
            type = ContentType.TEXT,
            topLevelOrdinal = 0,
            textContent = "First standalone text"
        )
        val container = LogContainer(
            logId = log.id,
            topLevelOrdinal = 1,
            layoutDirection = "HORIZONTAL"
        )
        val child1 = LogContentItem(
            logId = log.id,
            containerId = container.id,
            type = ContentType.IMAGE,
            itemOrdinal = 0,
            fileName = "img1.jpg"
        )
        val child2 = LogContentItem(
            logId = log.id,
            containerId = container.id,
            type = ContentType.AUDIO,
            itemOrdinal = 1,
            fileName = "aud1.m4a"
        )
        val standalone2 = LogContentItem(
            logId = log.id,
            type = ContentType.DOCUMENT,
            topLevelOrdinal = 2,
            fileName = "doc.pdf"
        )

        val logWithDetails = FeelingLogWithDetails(
            log = log,
            containers = listOf(container),
            contentItems = listOf(standalone2, child2, child1, standalone1)
        )

        val entries = logWithDetails.getTopLevelEntries()
        assertEquals(3, entries.size)

        assertTrue(entries[0] is TopLevelEntry.StandaloneItem)
        assertEquals(0, entries[0].topLevelOrdinal)
        assertEquals("First standalone text", (entries[0] as TopLevelEntry.StandaloneItem).item.textContent)

        assertTrue(entries[1] is TopLevelEntry.ContainerGroup)
        val group = entries[1] as TopLevelEntry.ContainerGroup
        assertEquals(1, group.topLevelOrdinal)
        assertEquals(2, group.childItems.size)
        assertEquals("img1.jpg", group.childItems[0].fileName)
        assertEquals("aud1.m4a", group.childItems[1].fileName)

        assertTrue(entries[2] is TopLevelEntry.StandaloneItem)
        assertEquals(2, entries[2].topLevelOrdinal)
        assertEquals("doc.pdf", (entries[2] as TopLevelEntry.StandaloneItem).item.fileName)
    }

    @Test
    fun tagNormalization_preservesDisplayCapitalization() {
        val rawTags = listOf("  Happy  ", "happy", "CALM ", "calm")
        val map = mutableMapOf<String, String>()
        rawTags.map { it.trim() }.forEach { raw ->
            val norm = raw.lowercase()
            if (!map.containsKey(norm)) {
                map[norm] = raw
            }
        }

        assertEquals(2, map.size)
        assertEquals("Happy", map["happy"])
        assertEquals("CALM", map["calm"])
    }

    @Test
    fun mediaReferenceCounting_acrossActiveAndSoftDeletedLogs_isCorrect() {
        val activeLog = FeelingLog(title = "Active Log", backgroundImageFileName = "shared_bg.jpg")
        val softDeletedLog = FeelingLog(
            title = "Soft Deleted Log",
            deletedAt = System.currentTimeMillis(),
            coverImageFileName = "shared_bg.jpg"
        )
        val permanentlyDeletedLogId = "deleted_id"

        val allLogs = listOf(
            FeelingLogWithDetails(log = activeLog),
            FeelingLogWithDetails(log = softDeletedLog)
        ).filter { it.log.id != permanentlyDeletedLogId }

        val targetFileName = "shared_bg.jpg"
        val isReferencedInContent = allLogs.any { log -> log.contentItems.any { item -> item.fileName == targetFileName } }
        val isReferencedInAtmosphere = allLogs.any { log ->
            log.log.backgroundImageFileName == targetFileName ||
            log.log.coverImageFileName == targetFileName ||
            log.log.themeAudioFileName == targetFileName
        }

        val isStillReferenced = isReferencedInContent || isReferencedInAtmosphere
        assertTrue(isStillReferenced)
    }
}
