package com.mi.explorer.data.model

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class FileCategory {
    FOLDER,
    IMAGE,
    AUDIO,
    VIDEO,
    DOCUMENT,
    ARCHIVE,
    APK,
    CODE,
    UNKNOWN
}

data class FileItem(
    val file: File,
    val name: String = file.name,
    val path: String = file.absolutePath,
    val isDirectory: Boolean = file.isDirectory,
    val size: Long = if (file.isDirectory) 0L else file.length(),
    val lastModified: Long = file.lastModified(),
    val isHidden: Boolean = file.isHidden || file.name.startsWith("."),
    val extension: String = if (file.isDirectory) "" else file.extension.lowercase(Locale.ROOT),
    val itemCount: Int = if (file.isDirectory) (file.listFiles()?.size ?: 0) else 0
) {
    val category: FileCategory
        get() = when {
            isDirectory -> FileCategory.FOLDER
            extension in listOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "heic") -> FileCategory.IMAGE
            extension in listOf("mp3", "wav", "ogg", "m4a", "flac", "aac", "wma") -> FileCategory.AUDIO
            extension in listOf("mp4", "mkv", "webm", "avi", "mov", "3gp") -> FileCategory.VIDEO
            extension in listOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "epub") -> FileCategory.DOCUMENT
            extension in listOf("zip", "rar", "7z", "tar", "gz") -> FileCategory.ARCHIVE
            extension in listOf("apk", "xapk", "apks") -> FileCategory.APK
            extension in listOf("kt", "java", "py", "js", "html", "css", "json", "xml", "c", "cpp", "sh", "md") -> FileCategory.CODE
            else -> FileCategory.UNKNOWN
        }

    val formattedSize: String
        get() = if (isDirectory) {
            "$itemCount item${if (itemCount == 1) "" else "s"}"
        } else {
            formatBytes(size)
        }

    val formattedDate: String
        get() {
            val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
            return sdf.format(Date(lastModified))
        }

    val timeGroup: String
        get() {
            val now = System.currentTimeMillis()
            val diff = now - lastModified
            val oneDay = 24 * 60 * 60 * 1000L
            return when {
                diff < oneDay -> "Today"
                diff < 2 * oneDay -> "Yesterday"
                diff < 7 * oneDay -> "Earlier this week"
                diff < 30 * oneDay -> "This month"
                else -> "Earlier"
            }
        }

    companion object {
        fun formatBytes(bytes: Long): String {
            if (bytes <= 0) return "0 B"
            val units = arrayOf("B", "KB", "MB", "GB", "TB")
            val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, units.size - 1)
            val value = bytes / Math.pow(1024.0, digitGroups.toDouble())
            return String.format(Locale.US, "%.1f %s", value, units[digitGroups])
        }
    }
}

data class StorageSpace(
    val totalBytes: Long,
    val freeBytes: Long,
    val usedBytes: Long
) {
    val usedPercentage: Float
        get() = if (totalBytes > 0) (usedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f

    val formattedTotal: String get() = FileItem.formatBytes(totalBytes)
    val formattedUsed: String get() = FileItem.formatBytes(usedBytes)
    val formattedFree: String get() = FileItem.formatBytes(freeBytes)
}

enum class SortType {
    NAME_ASC,
    NAME_DESC,
    DATE_NEWEST,
    DATE_OLDEST,
    SIZE_LARGEST,
    SIZE_SMALLEST,
    TYPE
}

enum class ViewMode {
    LIST,
    GRID
}

data class ClipboardState(
    val items: List<FileItem>,
    val isCut: Boolean = false
)
