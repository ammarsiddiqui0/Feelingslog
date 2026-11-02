package com.example.feelingslog

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

/**
 * Relation wrapper representing a complete log with its containers, content items, and tags.
 */
data class FeelingLogWithDetails(
    @Embedded
    val log: FeelingLog,

    @Relation(
        parentColumn = "id",
        entityColumn = "logId"
    )
    val containers: List<LogContainer> = emptyList(),

    @Relation(
        parentColumn = "id",
        entityColumn = "logId"
    )
    val contentItems: List<LogContentItem> = emptyList(),

    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = LogTagCrossRef::class,
            parentColumn = "logId",
            entityColumn = "tagId"
        )
    )
    val tags: List<Tag> = emptyList()
) {
    /**
     * Top-level entries sorted by topLevelOrdinal.
     */
    fun getTopLevelEntries(): List<TopLevelEntry> {
        val standaloneItems = contentItems
            .filter { it.containerId == null }
            .map { TopLevelEntry.StandaloneItem(it) }

        val containerEntries = containers.map { container ->
            val childItems = contentItems
                .filter { it.containerId == container.id }
                .sortedBy { it.itemOrdinal }
            TopLevelEntry.ContainerGroup(container, childItems)
        }

        return (standaloneItems + containerEntries).sortedBy { it.topLevelOrdinal }
    }
}

sealed class TopLevelEntry {
    abstract val topLevelOrdinal: Int

    data class StandaloneItem(val item: LogContentItem) : TopLevelEntry() {
        override val topLevelOrdinal: Int
            get() = item.topLevelOrdinal ?: 0
    }

    data class ContainerGroup(
        val container: LogContainer,
        val childItems: List<LogContentItem>
    ) : TopLevelEntry() {
        override val topLevelOrdinal: Int
            get() = container.topLevelOrdinal
    }
}
