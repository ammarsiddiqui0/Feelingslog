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

    suspend fun getLog(id: String): FeelingLog? {
        return logDao.getLogById(id)
    }

    suspend fun saveLog(log: FeelingLog) {
        // Update timestamp
        val updatedLog = log.copy(updatedAt = System.currentTimeMillis())
        logDao.insertLog(updatedLog)
    }

    suspend fun deleteLog(log: FeelingLog) {
        // Delete metadata
        logDao.deleteLog(log)
        
        // Delete associated files
        log.blocks.forEach { block ->
            when (block) {
                is LogContentBlock.Image -> deleteMediaFile(block.fileName)
                is LogContentBlock.Audio -> deleteMediaFile(block.fileName)
                else -> {}
            }
        }
    }

    private fun deleteMediaFile(fileName: String) {
        val file = File(mediaDir, fileName)
        if (file.exists()) file.delete()
    }

    /**
     * Helper to get the absolute path for a media file when needed by the UI.
     */
    fun getMediaPath(fileName: String): String {
        return File(mediaDir, fileName).absolutePath
    }
}
