package com.hassan.quizforge.extraction

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns

enum class FileType { PDF, DOCX, UNSUPPORTED }

object FileTypeDetector {

    private const val DOCX_MIME =
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"

    /** Best-effort display name for a content:// Uri, falling back to the last path segment. */
    fun queryDisplayName(context: Context, uri: Uri): String {
        runCatching {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0 && cursor.moveToFirst()) {
                    val name = cursor.getString(nameIndex)
                    if (!name.isNullOrBlank()) return name
                }
            }
        }
        return uri.lastPathSegment ?: "document"
    }

    fun detect(context: Context, uri: Uri, displayName: String): FileType {
        val mime = context.contentResolver.getType(uri)
        val lowerName = displayName.lowercase()
        return when {
            mime == "application/pdf" || lowerName.endsWith(".pdf") -> FileType.PDF
            mime == DOCX_MIME || lowerName.endsWith(".docx") -> FileType.DOCX
            else -> FileType.UNSUPPORTED
        }
    }

    fun extensionOf(displayName: String): String = displayName.substringAfterLast('.', "")
}
