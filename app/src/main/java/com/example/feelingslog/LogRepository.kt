package com.example.feelingslog

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.util.UUID

/**
 * Manages all log data and media files.
 */
class LogRepository(context: Context) {
    private val logDao = LogDatabase.getDatabase(context).logDao()
    private val mediaDir = File(context.filesDir, "media").apply {
        if (!exists()) mkdirs()
    }

    val allLogs: Flow<List<FeelingLog>> = logDao.getAllLogs()
    val allLogsWithDetails: Flow<List<FeelingLogWithDetails>> = logDao.getAllLogsWithDetails()

    val allCalendarLogs: Flow<List<FeelingLog>> = logDao.getAllActiveAndDeletedLogs()
    val allCalendarLogsWithDetails: Flow<List<FeelingLogWithDetails>> = logDao.getAllActiveAndDeletedLogsWithDetails()

    val deletedLogs: Flow<List<FeelingLog>> = logDao.getDeletedLogs()
    val deletedLogsWithDetails: Flow<List<FeelingLogWithDetails>> = logDao.getDeletedLogsWithDetails()

    val allTags: Flow<List<Tag>> = logDao.getAllTags()

    suspend fun getLog(id: String): FeelingLog? {
        return logDao.getLogById(id)
    }

    suspend fun getLogWithDetails(id: String): FeelingLogWithDetails? {
        return logDao.getLogWithDetailsById(id)
    }

    suspend fun saveLog(log: FeelingLog) {
        val updatedLog = log.copy(updatedAt = System.currentTimeMillis())
        logDao.insertLog(updatedLog)
    }

    suspend fun saveFullLog(
        log: FeelingLog,
        containers: List<LogContainer>,
        items: List<LogContentItem>,
        tags: List<Tag>
    ) {
        val updatedLog = log.copy(updatedAt = System.currentTimeMillis())
        logDao.saveFullLog(updatedLog, containers, items, tags)
    }

    suspend fun ungroupContainer(logId: String, containerId: String) {
        val logWithDetails = logDao.getLogWithDetailsById(logId) ?: return
        val targetContainer = logWithDetails.containers.find { it.id == containerId } ?: return
        val childItems = logWithDetails.contentItems.filter { it.containerId == containerId }.sortedBy { it.itemOrdinal }

        val newContainers = logWithDetails.containers.filter { it.id != containerId }
        val updatedItems = logWithDetails.contentItems.toMutableList()

        var insertOrdinal = targetContainer.topLevelOrdinal
        updatedItems.removeAll { it.containerId == containerId }
        
        childItems.forEach { child ->
            updatedItems.add(
                child.copy(
                    containerId = null,
                    topLevelOrdinal = insertOrdinal++,
                    itemOrdinal = 0
                )
            )
        }

        saveFullLog(logWithDetails.log, newContainers, updatedItems, logWithDetails.tags)
    }

    suspend fun deleteContainerWithContents(logId: String, containerId: String) {
        val logWithDetails = logDao.getLogWithDetailsById(logId) ?: return
        val newContainers = logWithDetails.containers.filter { it.id != containerId }
        val remainingItems = logWithDetails.contentItems.filter { it.containerId != containerId }
        saveFullLog(logWithDetails.log, newContainers, remainingItems, logWithDetails.tags)
    }

    suspend fun createContainerFromItems(logId: String, itemIds: List<String>, direction: String = "VERTICAL", size: String = "MEDIUM") {
        val logWithDetails = logDao.getLogWithDetailsById(logId) ?: return
        val newContainerId = UUID.randomUUID().toString()
        val nextTopLevelOrdinal = logWithDetails.getTopLevelEntries().size

        val newContainer = LogContainer(
            id = newContainerId,
            logId = logId,
            topLevelOrdinal = nextTopLevelOrdinal,
            layoutDirection = direction,
            size = size
        )

        val updatedContainers = logWithDetails.containers + newContainer
        val updatedItems = logWithDetails.contentItems.map { item ->
            if (item.id in itemIds) {
                val childOrdinal = logWithDetails.contentItems.filter { it.containerId == newContainerId }.size
                item.copy(
                    containerId = newContainerId,
                    topLevelOrdinal = null,
                    itemOrdinal = childOrdinal
                )
            } else {
                item
            }
        }

        saveFullLog(logWithDetails.log, updatedContainers, updatedItems, logWithDetails.tags)
    }

    suspend fun updateContainerSettings(logId: String, containerId: String, direction: String?, size: String?) {
        val logWithDetails = logDao.getLogWithDetailsById(logId) ?: return
        val updatedContainers = logWithDetails.containers.map { container ->
            if (container.id == containerId) {
                container.copy(
                    layoutDirection = direction ?: container.layoutDirection,
                    size = size ?: container.size
                )
            } else {
                container
            }
        }
        saveFullLog(logWithDetails.log, updatedContainers, logWithDetails.contentItems, logWithDetails.tags)
    }

    suspend fun softDeleteLog(log: FeelingLog) {
        if (log.deletedAt == null) {
            val updatedLog = log.copy(
                deletedAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            logDao.insertLog(updatedLog)
        }
    }

    suspend fun restoreLog(log: FeelingLog) {
        if (log.deletedAt != null) {
            val updatedLog = log.copy(
                deletedAt = null,
                updatedAt = System.currentTimeMillis()
            )
            logDao.insertLog(updatedLog)
        }
    }

    /**
     * Permanent deletion sequence:
     * 1. Capture file names from target log details BEFORE database deletion.
     * 2. Delete database records.
     * 3. Check remaining active AND Recently Deleted logs for references to each file name.
     * 4. Unlink physical file only when zero references remain across the entire database.
     */
    suspend fun permanentlyDeleteLog(log: FeelingLog) {
        val logWithDetails = logDao.getLogWithDetailsById(log.id)
        if (logWithDetails != null) {
            permanentlyDeleteLogWithDetails(logWithDetails)
        } else {
            logDao.deleteLog(log)
        }
    }

    suspend fun permanentlyDeleteLogWithDetails(logWithDetails: FeelingLogWithDetails) {
        // Step 1: Capture target file names before deletion
        val targetFileNames = mutableSetOf<String>()
        logWithDetails.contentItems.mapNotNull { it.fileName }.forEach { targetFileNames.add(it) }
        logWithDetails.log.backgroundImageFileName?.let { targetFileNames.add(it) }
        logWithDetails.log.coverImageFileName?.let { targetFileNames.add(it) }
        logWithDetails.log.themeAudioFileName?.let { targetFileNames.add(it) }

        // Step 2: Delete Room records
        logDao.deleteLog(logWithDetails.log)

        // Step 3 & 4: Check remaining active AND soft-deleted logs and unlink unreferenced files
        val remainingLogs = logDao.getAllActiveAndDeletedLogsWithDetailsList()
        targetFileNames.forEach { fileName ->
            val isReferencedInContent = remainingLogs.any { log -> log.contentItems.any { item -> item.fileName == fileName } }
            val isReferencedInAtmosphere = remainingLogs.any { log ->
                log.log.backgroundImageFileName == fileName ||
                log.log.coverImageFileName == fileName ||
                log.log.themeAudioFileName == fileName
            }

            if (!isReferencedInContent && !isReferencedInAtmosphere) {
                val file = File(mediaDir, fileName)
                if (file.exists()) file.delete()
            }
        }
    }

    suspend fun cleanupExpiredDeletedLogs(thresholdMs: Long = 30 * 24 * 60 * 60 * 1000L) {
        val thresholdTimestamp = System.currentTimeMillis() - thresholdMs
        val expiredLogs = logDao.getExpiredDeletedLogsWithDetails(thresholdTimestamp)
        expiredLogs.forEach { log ->
            permanentlyDeleteLogWithDetails(log)
        }
    }

    fun getMediaFile(fileName: String): File {
        return File(mediaDir, fileName)
    }

    fun getMediaPath(fileName: String): String {
        return getMediaFile(fileName).absolutePath
    }

    fun copyUriToLocalMedia(context: Context, uri: Uri, defaultPrefix: String = "file"): Pair<String, String>? {
        return try {
            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(uri) ?: "*/*"
            val extension = when {
                mimeType.contains("png") -> ".png"
                mimeType.contains("jpeg") || mimeType.contains("jpg") -> ".jpg"
                mimeType.contains("pdf") -> ".pdf"
                mimeType.contains("mp3") -> ".mp3"
                mimeType.contains("m4a") || mimeType.contains("mp4") -> ".m4a"
                mimeType.contains("word") || mimeType.contains("msword") -> ".doc"
                else -> {
                    val path = uri.path ?: ""
                    val dotIndex = path.lastIndexOf('.')
                    if (dotIndex != -1) path.substring(dotIndex) else ".bin"
                }
            }
            val fileName = "${defaultPrefix}_${UUID.randomUUID()}$extension"
            val destFile = File(mediaDir, fileName)

            contentResolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            Pair(fileName, mimeType)
        } catch (e: Exception) {
            null
        }
    }

    fun copyUriToInternalMedia(context: Context, uri: Uri, prefix: String = "file"): String? {
        return try {
            val extension = getExtensionFromUri(context, uri)
            val fileName = "${prefix}_${UUID.randomUUID()}$extension"
            val destinationFile = File(mediaDir, fileName)
            context.contentResolver.openInputStream(uri)?.use { input ->
                destinationFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            fileName
        } catch (e: Exception) {
            null
        }
    }

    private fun getExtensionFromUri(context: Context, uri: Uri): String {
        val mimeType = context.contentResolver.getType(uri)
        return when {
            mimeType?.contains("pdf", ignoreCase = true) == true -> ".pdf"
            mimeType?.contains("png", ignoreCase = true) == true -> ".png"
            mimeType?.contains("jpg", ignoreCase = true) == true || mimeType?.contains("jpeg", ignoreCase = true) == true -> ".jpg"
            mimeType?.contains("audio", ignoreCase = true) == true -> ".m4a"
            mimeType?.contains("word", ignoreCase = true) == true -> ".docx"
            else -> {
                val path = uri.path
                if (path != null && path.contains(".")) {
                    "." + path.substringAfterLast(".")
                } else ""
            }
        }
    }
}
