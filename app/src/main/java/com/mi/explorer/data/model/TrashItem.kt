package com.mi.explorer.data.model

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class TrashItem(
    val id: String,
    val originalPath: String,
    val trashFileName: String,
    val displayName: String,
    val size: Long,
    val deletedTimestamp: Long,
    val isDirectory: Boolean
) {
    val formattedDate: String
        get() {
            val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
            return sdf.format(Date(deletedTimestamp))
        }

    val formattedSize: String
        get() = FileItem.formatBytes(size)
}
