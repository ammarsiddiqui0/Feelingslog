package com.example.feelingslog

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

class LogViewModel(private val repository: LogRepository) : ViewModel() {

    // Subscribe to active logs with details
    val logsWithDetails: StateFlow<List<FeelingLogWithDetails>> = repository.allLogsWithDetails.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val logs: StateFlow<List<FeelingLog>> = repository.allLogs.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Subscribe to active & soft-deleted logs for Calendar
    val calendarLogsWithDetails: StateFlow<List<FeelingLogWithDetails>> = repository.allCalendarLogsWithDetails.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val calendarLogs: StateFlow<List<FeelingLog>> = repository.allCalendarLogs.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Subscribe to soft-deleted logs for Recently Deleted
    val deletedLogsWithDetails: StateFlow<List<FeelingLogWithDetails>> = repository.deletedLogsWithDetails.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val deletedLogs: StateFlow<List<FeelingLog>> = repository.deletedLogs.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allTags: StateFlow<List<Tag>> = repository.allTags.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        cleanupExpiredLogs()
        seedDataIfEmpty()
    }

    fun cleanupExpiredLogs(thresholdMs: Long = 30 * 24 * 60 * 60 * 1000L) {
        viewModelScope.launch {
            repository.cleanupExpiredDeletedLogs(thresholdMs)
        }
    }

    private fun seedDataIfEmpty() {
        viewModelScope.launch {
            val currentLogs = repository.allLogs.first()
            if (currentLogs.isEmpty()) {
                val samples = listOf(
                    Triple("Morning Walk", "The air was so fresh today.", listOf("happy", "calm")),
                    Triple("Coffee with Friend", "Caught up with Sarah after a long time.", listOf("happy")),
                    Triple("Rainy Afternoon", "Reading a book while it pours outside.", listOf("calm")),
                    Triple("Gym Session", "New PR today! Feeling strong.", listOf("excited", "happy"))
                )
                samples.forEach { (title, text, tagList) ->
                    val log = FeelingLog(title = title)
                    val item = LogContentItem(
                        logId = log.id,
                        type = ContentType.TEXT,
                        topLevelOrdinal = 0,
                        textContent = text
                    )
                    val tags = tagList.map { tagName ->
                        Tag(
                            id = UUID.nameUUIDFromBytes(tagName.trim().lowercase().toByteArray()).toString(),
                            name = tagName
                        )
                    }
                    repository.saveFullLog(log, emptyList(), listOf(item), tags)
                }
            }
        }
    }

    suspend fun getLog(id: String): FeelingLog? {
        return repository.getLog(id)
    }

    suspend fun getLogWithDetails(id: String): FeelingLogWithDetails? {
        return repository.getLogWithDetails(id)
    }

    fun copyUriToInternalMedia(context: Context, uri: Uri, prefix: String): String? {
        return repository.copyUriToInternalMedia(context, uri, prefix)
    }

    fun getMediaPath(fileName: String): String {
        return repository.getMediaPath(fileName)
    }

    fun copyUriToLocalMedia(context: Context, uri: Uri, prefix: String): Pair<String, String>? {
        return repository.copyUriToLocalMedia(context, uri, prefix)
    }

    fun getMediaFile(fileName: String): File {
        return repository.getMediaFile(fileName)
    }

    fun saveLog(log: FeelingLog) {
        viewModelScope.launch {
            repository.saveLog(log)
        }
    }

    fun saveFullLog(
        log: FeelingLog,
        containers: List<LogContainer>,
        items: List<LogContentItem>,
        tags: List<Tag>
    ) {
        viewModelScope.launch {
            repository.saveFullLog(log, containers, items, tags)
        }
    }

    fun ungroupContainer(logId: String, containerId: String) {
        viewModelScope.launch {
            repository.ungroupContainer(logId, containerId)
        }
    }

    fun deleteContainerWithContents(logId: String, containerId: String) {
        viewModelScope.launch {
            repository.deleteContainerWithContents(logId, containerId)
        }
    }

    fun createContainerFromItems(logId: String, itemIds: List<String>, direction: String = "VERTICAL", size: String = "MEDIUM") {
        viewModelScope.launch {
            repository.createContainerFromItems(logId, itemIds, direction, size)
        }
    }

    fun updateContainerSettings(logId: String, containerId: String, direction: String?, size: String?) {
        viewModelScope.launch {
            repository.updateContainerSettings(logId, containerId, direction, size)
        }
    }

    fun addLog(title: String, content: String) {
        val log = FeelingLog(title = title)
        val item = LogContentItem(
            logId = log.id,
            type = ContentType.TEXT,
            topLevelOrdinal = 0,
            textContent = content
        )
        saveFullLog(log, emptyList(), listOf(item), emptyList())
    }

    fun updateLog(log: FeelingLog, title: String, content: String) {
        viewModelScope.launch {
            val existingDetails = repository.getLogWithDetails(log.id)
            val updatedLog = log.copy(title = title)
            if (existingDetails != null) {
                val items = existingDetails.contentItems.toMutableList()
                val textIndex = items.indexOfFirst { it.type == ContentType.TEXT && it.containerId == null }
                if (textIndex != -1) {
                    items[textIndex] = items[textIndex].copy(textContent = content)
                } else {
                    items.add(
                        0,
                        LogContentItem(
                            logId = log.id,
                            type = ContentType.TEXT,
                            topLevelOrdinal = 0,
                            textContent = content
                        )
                    )
                }
                repository.saveFullLog(updatedLog, existingDetails.containers, items, existingDetails.tags)
            } else {
                val item = LogContentItem(
                    logId = log.id,
                    type = ContentType.TEXT,
                    topLevelOrdinal = 0,
                    textContent = content
                )
                repository.saveFullLog(updatedLog, emptyList(), listOf(item), emptyList())
            }
        }
    }

    fun softDeleteLog(log: FeelingLog) {
        viewModelScope.launch {
            repository.softDeleteLog(log)
        }
    }

    fun restoreLog(log: FeelingLog) {
        viewModelScope.launch {
            repository.restoreLog(log)
        }
    }

    fun permanentlyDeleteLog(log: FeelingLog) {
        viewModelScope.launch {
            repository.permanentlyDeleteLog(log)
        }
    }
}
