package com.example.feelingslog

import android.content.Context
import kotlinx.coroutines.flow.Flow
import java.io.File

/**
 * Manages all log data and media files.
 */
class LogRepository(context: Context) {
    private val logDao = LogDatabase.getDatabase(context).logDao()
    private val mediaDir = File(context.filesDir, "media").apply {
        if (!exists()) mkdirs()
    }

    val allLogs: Flow<List<FeelingLog>> = logDao.getAllLogs()
    val allCalendarLogs: Flow<List<FeelingLog>> = logDao.getAllActiveAndDeletedLogs()
    val deletedLogs: Flow<List<FeelingLog>> = logDao.getDeletedLogs()

    suspend fun getLog(id: String): FeelingLog? {
        return logDao.getLogById(id)
    }

    suspend fun saveLog(log: FeelingLog) {
        val updatedLog = log.copy(updatedAt = System.currentTimeMillis())
        logDao.insertLog(updatedLog)
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

    suspend fun permanentlyDeleteLog(log: FeelingLog) {
        // Delete entity from Room
        logDao.deleteLog(log)
        
        // Safely delete media files only if not referenced by another log
        log.blocks.forEach { block ->
            when (block) {
                is LogContentBlock.Image -> deleteMediaFileIfUnused(block.fileName, log.id)
                is LogContentBlock.Audio -> deleteMediaFileIfUnused(block.fileName, log.id)
                else -> {}
            }
        }
    }

    suspend fun cleanupExpiredDeletedLogs(thresholdMs: Long = 30 * 24 * 60 * 60 * 1000L) {
        val thresholdTimestamp = System.currentTimeMillis() - thresholdMs
        val expiredLogs = logDao.getExpiredDeletedLogs(thresholdTimestamp)
        expiredLogs.forEach { log ->
            permanentlyDeleteLog(log)
        }
    }

    private suspend fun deleteMediaFileIfUnused(fileName: String, deletedLogId: String) {
        val remainingLogs = logDao.getAllLogsList().filter { it.id != deletedLogId }
        val isUsed = remainingLogs.any { otherLog ->
            otherLog.blocks.any { block ->
                when (block) {
                    is LogContentBlock.Image -> block.fileName == fileName
                    is LogContentBlock.Audio -> block.fileName == fileName
                    else -> false
                }
            }
        }
        if (!isUsed) {
            val file = File(mediaDir, fileName)
            if (file.exists()) file.delete()
        }
    }

    /**
     * Helper to get the absolute path for a media file when needed by the UI.
     */
    fun getMediaPath(fileName: String): String {
        return File(mediaDir, fileName).absolutePath
    }
}
