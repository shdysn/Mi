package com.mi.explorer.data.repository

import android.content.Context
import android.os.Environment
import android.os.StatFs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.mi.explorer.data.model.FileCategory
import com.mi.explorer.data.model.FileItem
import com.mi.explorer.data.model.SortType
import com.mi.explorer.data.model.StorageSpace
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class CleanScanResult(
    val junkFiles: List<FileItem>,
    val largeFiles: List<FileItem>,
    val apkFiles: List<FileItem>,
    val emptyFolders: List<FileItem>
) {
    val totalJunkBytes: Long get() = junkFiles.sumOf { it.size }
    val totalLargeBytes: Long get() = largeFiles.sumOf { it.size }
    val totalApkBytes: Long get() = apkFiles.sumOf { it.size }
    val formattedJunkSize: String get() = FileItem.formatBytes(totalJunkBytes)
}

class FileRepository(private val context: Context) {

    val rootStorageDirectory: File
        get() = try {
            val ext = Environment.getExternalStorageDirectory()
            if (ext.exists() && ext.canRead()) ext else context.filesDir
        } catch (e: Exception) {
            context.filesDir
        }

    val downloadsDirectory: File
        get() = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).let {
            if (it.exists()) it else File(rootStorageDirectory, "Download")
        }

    val documentsDirectory: File
        get() = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).let {
            if (it.exists()) it else File(rootStorageDirectory, "Documents")
        }

    val picturesDirectory: File
        get() = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).let {
            if (it.exists()) it else File(rootStorageDirectory, "Pictures")
        }

    val dcimDirectory: File
        get() = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).let {
            if (it.exists()) it else File(rootStorageDirectory, "DCIM")
        }

    val musicDirectory: File
        get() = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).let {
            if (it.exists()) it else File(rootStorageDirectory, "Music")
        }

    val moviesDirectory: File
        get() = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).let {
            if (it.exists()) it else File(rootStorageDirectory, "Movies")
        }

    init {
        ensureMiExplorerSampleData()
    }

    private fun ensureMiExplorerSampleData() {
        try {
            val base = File(context.filesDir, "MiExplorer")
            if (!base.exists()) {
                base.mkdirs()
                File(base, "Welcome_Mi_Explorer.txt").writeText(
                    """
                    Welcome to Mi Explorer!
                    
                    Inspired by Xiaomi MIUI / HyperOS File Manager:
                    • Recent tab: Quick access to recently created, captured, or downloaded items.
                    • Storage tab: Clear overview of used space with 8 fast category shortcuts.
                    • Deep Clean: Scan for app cache, residual junk, obsolete APKs, and large files.
                    • Built-in Tools: Text Editor, Photo Previewer, APK Inspector, and ZIP manager.
                    • 100% Free: No ads, no subscriptions, fast and lightweight.
                    """.trimIndent()
                )
                File(base, "MIUI_Tips.md").writeText(
                    """
                    # MIUI File Manager Tips
                    - Switch between Recent and Storage using the top tabs.
                    - Tap the Cleaner button to free up gigabytes of space in seconds.
                    - Use FTP server mode to transfer files directly to your PC without cables!
                    """.trimIndent()
                )
                val docs = File(base, "Documents")
                docs.mkdirs()
                File(docs, "Project_Roadmap.txt").writeText("1. Modern Compose UI\n2. MIUI-inspired Squircles\n3. Zero Ads\n4. Lightning fast\n")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun listFiles(
        directory: File,
        showHidden: Boolean = false,
        sortType: SortType = SortType.NAME_ASC,
        searchQuery: String = ""
    ): List<FileItem> = withContext(Dispatchers.IO) {
        val files = directory.listFiles() ?: return@withContext emptyList()
        var items = files.map { FileItem(it) }

        if (!showHidden) {
            items = items.filter { !it.isHidden }
        }

        if (searchQuery.isNotBlank()) {
            val query = searchQuery.trim().lowercase()
            items = items.filter { it.name.lowercase().contains(query) }
        }

        val folders = items.filter { it.isDirectory }
        val normalFiles = items.filter { !it.isDirectory }

        val sortedFolders = sortList(folders, sortType)
        val sortedFiles = sortList(normalFiles, sortType)

        sortedFolders + sortedFiles
    }

    private fun sortList(list: List<FileItem>, sortType: SortType): List<FileItem> {
        return when (sortType) {
            SortType.NAME_ASC -> list.sortedBy { it.name.lowercase() }
            SortType.NAME_DESC -> list.sortedByDescending { it.name.lowercase() }
            SortType.DATE_NEWEST -> list.sortedByDescending { it.lastModified }
            SortType.DATE_OLDEST -> list.sortedBy { it.lastModified }
            SortType.SIZE_LARGEST -> list.sortedByDescending { it.size }
            SortType.SIZE_SMALLEST -> list.sortedBy { it.size }
            SortType.TYPE -> list.sortedWith(compareBy({ it.extension }, { it.name.lowercase() }))
        }
    }

    suspend fun getRecentFiles(): List<FileItem> = withContext(Dispatchers.IO) {
        val roots = listOfNotNull(
            rootStorageDirectory,
            context.filesDir,
            context.getExternalFilesDir(null)
        ).distinct()

        val results = mutableListOf<FileItem>()
        for (root in roots) {
            scanFiles(root, results, maxFiles = 100, maxDepth = 4)
        }
        results.sortedByDescending { it.lastModified }
    }

    private fun scanFiles(dir: File, results: MutableList<FileItem>, maxFiles: Int, maxDepth: Int, currentDepth: Int = 0) {
        if (currentDepth > maxDepth || results.size >= maxFiles) return
        val list = dir.listFiles() ?: return
        for (f in list) {
            if (f.isDirectory) {
                if (!f.name.startsWith(".")) {
                    scanFiles(f, results, maxFiles, maxDepth, currentDepth + 1)
                }
            } else {
                if (!f.name.startsWith(".")) {
                    results.add(FileItem(f))
                }
            }
        }
    }

    suspend fun getCategoryFiles(category: FileCategory): List<FileItem> = withContext(Dispatchers.IO) {
        val roots = listOfNotNull(
            rootStorageDirectory,
            context.filesDir,
            context.getExternalFilesDir(null)
        ).distinct()

        val results = mutableListOf<FileItem>()
        for (root in roots) {
            scanCategory(root, category, results, maxDepth = 4)
        }
        results.sortedByDescending { it.lastModified }
    }

    private fun scanCategory(dir: File, category: FileCategory, results: MutableList<FileItem>, maxDepth: Int, currentDepth: Int = 0) {
        if (currentDepth > maxDepth || results.size >= 250) return
        val list = dir.listFiles() ?: return
        for (f in list) {
            if (f.isDirectory) {
                if (!f.name.startsWith(".")) {
                    scanCategory(f, category, results, maxDepth, currentDepth + 1)
                }
            } else {
                val item = FileItem(f)
                if (item.category == category) {
                    results.add(item)
                }
            }
        }
    }

    suspend fun scanForClean(): CleanScanResult = withContext(Dispatchers.IO) {
        val roots = listOfNotNull(
            rootStorageDirectory,
            context.filesDir,
            context.getExternalFilesDir(null)
        ).distinct()

        val junk = mutableListOf<FileItem>()
        val large = mutableListOf<FileItem>()
        val apks = mutableListOf<FileItem>()
        val emptyFolders = mutableListOf<FileItem>()

        for (root in roots) {
            scanCleanRecursively(root, junk, large, apks, emptyFolders, depth = 0, maxDepth = 4)
        }

        CleanScanResult(
            junkFiles = junk,
            largeFiles = large.sortedByDescending { it.size },
            apkFiles = apks.sortedByDescending { it.size },
            emptyFolders = emptyFolders
        )
    }

    private fun scanCleanRecursively(
        dir: File,
        junk: MutableList<FileItem>,
        large: MutableList<FileItem>,
        apks: MutableList<FileItem>,
        emptyFolders: MutableList<FileItem>,
        depth: Int,
        maxDepth: Int
    ) {
        if (depth > maxDepth) return
        val files = dir.listFiles() ?: return
        if (files.isEmpty() && dir != rootStorageDirectory) {
            emptyFolders.add(FileItem(dir))
            return
        }
        for (f in files) {
            if (f.isDirectory) {
                scanCleanRecursively(f, junk, large, apks, emptyFolders, depth + 1, maxDepth)
            } else {
                val item = FileItem(f)
                val ext = item.extension
                if (ext in listOf("tmp", "temp", "log", "thumb", "bak") || f.name.contains("cache", ignoreCase = true)) {
                    junk.add(item)
                }
                if (item.size > 15L * 1024 * 1024) { // > 15MB
                    large.add(item)
                }
                if (ext == "apk") {
                    apks.add(item)
                }
            }
        }
    }

    suspend fun getStorageSpace(): StorageSpace = withContext(Dispatchers.IO) {
        try {
            val path = rootStorageDirectory.path
            val stat = StatFs(path)
            val blockSize = stat.blockSizeLong
            val totalBlocks = stat.blockCountLong
            val availableBlocks = stat.availableBlocksLong

            val totalBytes = totalBlocks * blockSize
            val freeBytes = availableBlocks * blockSize
            val usedBytes = (totalBytes - freeBytes).coerceAtLeast(0L)

            StorageSpace(
                totalBytes = totalBytes,
                freeBytes = freeBytes,
                usedBytes = usedBytes
            )
        } catch (e: Exception) {
            val total = 64L * 1024 * 1024 * 1024
            val used = 26L * 1024 * 1024 * 1024
            StorageSpace(totalBytes = total, freeBytes = total - used, usedBytes = used)
        }
    }

    suspend fun createFolder(parent: File, name: String): Result<File> = withContext(Dispatchers.IO) {
        try {
            val sanitized = name.trim().replace("/", "")
            if (sanitized.isEmpty()) return@withContext Result.failure(IllegalArgumentException("Name cannot be empty"))
            val target = File(parent, sanitized)
            if (target.exists()) return@withContext Result.failure(IllegalStateException("A folder with this name already exists"))
            if (target.mkdirs()) Result.success(target) else Result.failure(Exception("Failed to create folder"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createTextFile(parent: File, name: String, initialContent: String = ""): Result<File> = withContext(Dispatchers.IO) {
        try {
            val sanitized = name.trim().replace("/", "")
            if (sanitized.isEmpty()) return@withContext Result.failure(IllegalArgumentException("Name cannot be empty"))
            val target = File(parent, sanitized)
            if (target.exists()) return@withContext Result.failure(IllegalStateException("A file with this name already exists"))
            target.writeText(initialContent)
            Result.success(target)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun delete(item: FileItem): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val success = if (item.isDirectory) item.file.deleteRecursively() else item.file.delete()
            Result.success(success)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun rename(item: FileItem, newName: String): Result<File> = withContext(Dispatchers.IO) {
        try {
            val sanitized = newName.trim().replace("/", "")
            if (sanitized.isEmpty()) return@withContext Result.failure(IllegalArgumentException("New name cannot be empty"))
            val newFile = File(item.file.parentFile, sanitized)
            if (newFile.exists()) return@withContext Result.failure(IllegalStateException("Target already exists"))
            val success = item.file.renameTo(newFile)
            if (success) Result.success(newFile) else Result.failure(Exception("Rename failed"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun copy(sources: List<FileItem>, destinationDir: File): Result<Int> = withContext(Dispatchers.IO) {
        try {
            var count = 0
            for (source in sources) {
                val dest = File(destinationDir, source.name)
                if (source.isDirectory) {
                    source.file.copyRecursively(dest, overwrite = true)
                } else {
                    source.file.copyTo(dest, overwrite = true)
                }
                count++
            }
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun move(sources: List<FileItem>, destinationDir: File): Result<Int> = withContext(Dispatchers.IO) {
        try {
            var count = 0
            for (source in sources) {
                val dest = File(destinationDir, source.name)
                val moved = source.file.renameTo(dest)
                if (!moved) {
                    if (source.isDirectory) {
                        source.file.copyRecursively(dest, overwrite = true)
                        source.file.deleteRecursively()
                    } else {
                        source.file.copyTo(dest, overwrite = true)
                        source.file.delete()
                    }
                }
                count++
            }
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun readText(file: File): Result<String> = withContext(Dispatchers.IO) {
        try {
            Result.success(file.readText())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun writeText(file: File, content: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            file.writeText(content)
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun zip(items: List<FileItem>, zipOutputFile: File): Result<File> = withContext(Dispatchers.IO) {
        try {
            ZipOutputStream(FileOutputStream(zipOutputFile)).use { zos ->
                for (item in items) {
                    addToZip("", item.file, zos)
                }
            }
            Result.success(zipOutputFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun addToZip(basePath: String, file: File, zos: ZipOutputStream) {
        val entryName = if (basePath.isEmpty()) file.name else "$basePath/${file.name}"
        if (file.isDirectory) {
            zos.putNextEntry(ZipEntry("$entryName/"))
            zos.closeEntry()
            file.listFiles()?.forEach { child ->
                addToZip(entryName, child, zos)
            }
        } else {
            zos.putNextEntry(ZipEntry(entryName))
            FileInputStream(file).use { fis ->
                fis.copyTo(zos)
            }
            zos.closeEntry()
        }
    }

    suspend fun unzip(zipFile: File, outputDir: File): Result<File> = withContext(Dispatchers.IO) {
        try {
            ZipInputStream(FileInputStream(zipFile)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val destFile = File(outputDir, entry.name)
                    if (entry.isDirectory) {
                        destFile.mkdirs()
                    } else {
                        destFile.parentFile?.mkdirs()
                        FileOutputStream(destFile).use { fos ->
                            zis.copyTo(fos)
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
            Result.success(outputDir)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
