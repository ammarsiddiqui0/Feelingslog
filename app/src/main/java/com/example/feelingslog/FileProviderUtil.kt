package com.example.feelingslog

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

object FileProviderUtil {
    fun openDocument(context: Context, file: File, mimeType: String? = null) {
        if (!file.exists()) return
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType ?: getMimeType(file.name))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(intent, "Open File")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    private fun getMimeType(fileName: String): String {
        return when {
            fileName.endsWith(".pdf", ignoreCase = true) -> "application/pdf"
            fileName.endsWith(".doc", ignoreCase = true) -> "application/msword"
            fileName.endsWith(".docx", ignoreCase = true) -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            fileName.endsWith(".png", ignoreCase = true) -> "image/png"
            fileName.endsWith(".jpg", ignoreCase = true) || fileName.endsWith(".jpeg", ignoreCase = true) -> "image/jpeg"
            fileName.endsWith(".m4a", ignoreCase = true) || fileName.endsWith(".mp3", ignoreCase = true) -> "audio/*"
            else -> "*/*"
        }
    }
}
