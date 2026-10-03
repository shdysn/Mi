package com.mi.explorer.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.mi.explorer.data.model.*
import com.mi.explorer.data.repository.*
import com.mi.explorer.ui.components.MiTab
import java.io.File

enum class Screen {
    MAIN,
    CLEANER,
    FTP_SERVER,
    CATEGORY_VIEW,
    TEXT_EDITOR,
    IMAGE_VIEWER,
    APP_MANAGER,
    VAULT,
    DUPLICATES,
    STORAGE_ANALYZER,
    ZIP_VIEWER
}

data class ZipViewerState(
    val archiveInfo: ZipArchiveInfo? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

data class StorageTabState(
    val currentDir: File,
    val backStack: List<File> = emptyList(),
    val forwardStack: List<File> = emptyList(),
    val items: List<FileItem> = emptyList(),
    val selectedItems: Set<FileItem> = emptySet(),
    val viewMode: ViewMode = ViewMode.LIST,
    val sortType: SortType = SortType.NAME_ASC,
    val searchQuery: String = "",
    val showHidden: Boolean = false,
    val isLoading: Boolean = false
) {
    val isSelectionMode: Boolean get() = selectedItems.isNotEmpty()
}

data class TextEditorState(
    val file: File? = null,
    val content: String = "",
    val originalContent: String = "",
    val wordWrap: Boolean = false,
    val isSaving: Boolean = false
) {
    val isModified: Boolean get() = content != originalContent
    val lineCount: Int get() = if (content.isEmpty()) 1 else content.lines().size
    val charCount: Int get() = content.length
}

data class ImageViewerState(
    val currentFile: File? = null,
    val imageList: List<FileItem> = emptyList(),
    val currentIndex: Int = 0
)

data class CategoryViewState(
    val category: FileCategory = FileCategory.IMAGE,
    val title: String = "",
    val items: List<FileItem> = emptyList(),
    val isLoading: Boolean = false
)

data class FtpServerState(
    val isRunning: Boolean = false,
    val port: Int = 2121,
    val ipAddress: String = "192.168.1.108"
) {
    val url: String get() = "ftp://$ipAddress:$port"
}

class ExplorerViewModel(application: Application) : AndroidViewModel(application) {

    val fileRepository = FileRepository(application.applicationContext)
    val appsRepository = AppsRepository(application.applicationContext)

    // Current Screen
    private val _currentScreen = MutableStateFlow(Screen.MAIN)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val screenBackStack = mutableListOf<Screen>()

    // Current Main Tab (Recent vs Storage)
    private val _selectedTab = MutableStateFlow(MiTab.STORAGE)
    val selectedTab: StateFlow<MiTab> = _selectedTab.asStateFlow()

    // Recent Files State
    private val _recentFiles = MutableStateFlow<List<FileItem>>(emptyList())
    val recentFiles: StateFlow<List<FileItem>> = _recentFiles.asStateFlow()
    val isRecentLoading = MutableStateFlow(false)

    // Storage Tab State
    private val initialDir = fileRepository.rootStorageDirectory
    private val _storageState = MutableStateFlow(StorageTabState(currentDir = initialDir))
    val storageState: StateFlow<StorageTabState> = _storageState.asStateFlow()

    // Storage capacity overview
    private val _storageSpace = MutableStateFlow(
        StorageSpace(totalBytes = 64L * 1024 * 1024 * 1024, freeBytes = 38L * 1024 * 1024 * 1024, usedBytes = 26L * 1024 * 1024 * 1024)
    )
    val storageSpace: StateFlow<StorageSpace> = _storageSpace.asStateFlow()

    // Clipboard
    private val _clipboard = MutableStateFlow<ClipboardState?>(null)
    val clipboard: StateFlow<ClipboardState?> = _clipboard.asStateFlow()

    // Cleaner State
    private val _cleanScan = MutableStateFlow<CleanScanResult?>(null)
    val cleanScan: StateFlow<CleanScanResult?> = _cleanScan.asStateFlow()
    val isCleaning = MutableStateFlow(false)
    val isCleanScanning = MutableStateFlow(false)
    val cleanedBytes = MutableStateFlow<Long?>(null)

    // FTP Server State
    private val _ftpServerState = MutableStateFlow(FtpServerState())
    val ftpServerState: StateFlow<FtpServerState> = _ftpServerState.asStateFlow()

    // Text Editor State
    private val _textEditorState = MutableStateFlow(TextEditorState())
    val textEditorState: StateFlow<TextEditorState> = _textEditorState.asStateFlow()

    // Image Viewer State
    private val _imageViewerState = MutableStateFlow(ImageViewerState())
    val imageViewerState: StateFlow<ImageViewerState> = _imageViewerState.asStateFlow()

    // Category View State
    private val _categoryViewState = MutableStateFlow(CategoryViewState())
    val categoryViewState: StateFlow<CategoryViewState> = _categoryViewState.asStateFlow()

    // App Manager State
    private val _installedApps = MutableStateFlow<List<AppInfoItem>>(emptyList())
    val installedApps: StateFlow<List<AppInfoItem>> = _installedApps.asStateFlow()
    val isAppsLoading = MutableStateFlow(false)
    val includeSystemApps = MutableStateFlow(false)
    val appsSearchQuery = MutableStateFlow("")

    // Vault Repository & State
    val vaultRepository = VaultRepository(application)
    val isVaultPinSet = MutableStateFlow(vaultRepository.isPinSet())
    val isVaultUnlocked = MutableStateFlow(false)
    private val _vaultFiles = MutableStateFlow<List<FileItem>>(emptyList())
    val vaultFiles: StateFlow<List<FileItem>> = _vaultFiles.asStateFlow()
    val isVaultLoading = MutableStateFlow(false)

    // Duplicate Repository & State
    val duplicateRepository = DuplicateRepository(application)
    private val _duplicateScanResult = MutableStateFlow<DuplicateScanResult?>(null)
    val duplicateScanResult: StateFlow<DuplicateScanResult?> = _duplicateScanResult.asStateFlow()
    val isDuplicateScanning = MutableStateFlow(false)
    val selectedDuplicateFiles = MutableStateFlow<Set<FileItem>>(emptySet())

    // Storage Analyzer Repository & State
    val storageAnalyzerRepository = StorageAnalyzerRepository(application)
    private val _storageAnalysisResult = MutableStateFlow<StorageAnalysisResult?>(null)
    val storageAnalysisResult: StateFlow<StorageAnalysisResult?> = _storageAnalysisResult.asStateFlow()
    val isStorageAnalyzing = MutableStateFlow(false)

    // Zip Viewer & Compressor State
    val zipRepository = ZipRepository(application)
    private val _zipViewerState = MutableStateFlow(ZipViewerState())
    val zipViewerState: StateFlow<ZipViewerState> = _zipViewerState.asStateFlow()
    val isZipExtracting = MutableStateFlow(false)

    // AMOLED Pitch Black Mode State
    val isAmoledMode = MutableStateFlow(false)

    // Snackbar message
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        refreshStorage()
        loadDirectory(initialDir)
        loadRecentFiles()
    }

    fun selectTab(tab: MiTab) {
        _selectedTab.value = tab
        if (tab == MiTab.RECENT && _recentFiles.value.isEmpty()) {
            loadRecentFiles()
        }
    }

    fun navigateToScreen(screen: Screen) {
        if (_currentScreen.value != screen) {
            screenBackStack.add(_currentScreen.value)
            _currentScreen.value = screen
        }
    }

    fun handleBackPress(): Boolean {
        if (_currentScreen.value == Screen.MAIN) {
            val state = _storageState.value
            if (state.isSelectionMode) {
                clearSelection()
                return true
            }
            if (_selectedTab.value == MiTab.STORAGE && state.backStack.isNotEmpty()) {
                goBackInDirectory()
                return true
            }
            if (_selectedTab.value == MiTab.RECENT) {
                _selectedTab.value = MiTab.STORAGE
                return true
            }
            return false
        }

        if (screenBackStack.isNotEmpty()) {
            _currentScreen.value = screenBackStack.removeAt(screenBackStack.lastIndex)
            return true
        }

        _currentScreen.value = Screen.MAIN
        return true
    }

    fun refreshStorage() {
        viewModelScope.launch {
            _storageSpace.value = fileRepository.getStorageSpace()
        }
    }

    fun onStoragePermissionGranted() {
        refreshStorage()
        val currentDir = _storageState.value.currentDir
        val targetDir = if (currentDir.exists() && currentDir.canRead()) {
            currentDir
        } else {
            fileRepository.rootStorageDirectory
        }
        loadDirectory(targetDir)
        loadRecentFiles()
    }

    fun loadRecentFiles() {
        viewModelScope.launch {
            isRecentLoading.value = true
            val files = fileRepository.getRecentFiles()
            _recentFiles.value = files
            isRecentLoading.value = false
        }
    }

    fun loadDirectory(dir: File, addToHistory: Boolean = false) {
        viewModelScope.launch {
            val current = _storageState.value
            val newBackStack = if (addToHistory && current.currentDir != dir) {
                current.backStack + current.currentDir
            } else {
                current.backStack
            }

            _storageState.update {
                it.copy(
                    currentDir = dir,
                    backStack = newBackStack,
                    forwardStack = emptyList(),
                    selectedItems = emptySet(),
                    isLoading = true
                )
            }

            val items = fileRepository.listFiles(
                directory = dir,
                showHidden = current.showHidden,
                sortType = current.sortType,
                searchQuery = current.searchQuery
            )

            _storageState.update {
                it.copy(items = items, isLoading = false)
            }
        }
    }

    fun goBackInDirectory() {
        val current = _storageState.value
        if (current.backStack.isNotEmpty()) {
            val prev = current.backStack.last()
            val newBackStack = current.backStack.dropLast(1)
            val newForwardStack = current.forwardStack + current.currentDir

            viewModelScope.launch {
                val items = fileRepository.listFiles(
                    directory = prev,
                    showHidden = current.showHidden,
                    sortType = current.sortType,
                    searchQuery = current.searchQuery
                )
                _storageState.update {
                    it.copy(
                        currentDir = prev,
                        backStack = newBackStack,
                        forwardStack = newForwardStack,
                        items = items,
                        selectedItems = emptySet()
                    )
                }
            }
        }
    }

    fun refreshCurrentDirectory() {
        loadDirectory(_storageState.value.currentDir, addToHistory = false)
        refreshStorage()
        loadRecentFiles()
    }

    fun toggleViewMode() {
        _storageState.update {
            it.copy(viewMode = if (it.viewMode == ViewMode.LIST) ViewMode.GRID else ViewMode.LIST)
        }
    }

    fun setSortType(sortType: SortType) {
        _storageState.update { it.copy(sortType = sortType) }
        loadDirectory(_storageState.value.currentDir)
    }

    fun toggleShowHidden() {
        val newShow = !_storageState.value.showHidden
        _storageState.update { it.copy(showHidden = newShow) }
        loadDirectory(_storageState.value.currentDir)
    }

    fun setSearchQuery(query: String) {
        _storageState.update { it.copy(searchQuery = query) }
        loadDirectory(_storageState.value.currentDir)
    }

    fun toggleSelectItem(item: FileItem) {
        _storageState.update { state ->
            val set = state.selectedItems.toMutableSet()
            if (set.contains(item)) set.remove(item) else set.add(item)
            state.copy(selectedItems = set)
        }
    }

    fun selectAll() {
        _storageState.update { state ->
            state.copy(selectedItems = state.items.toSet())
        }
    }

    fun clearSelection() {
        _storageState.update { state ->
            state.copy(selectedItems = emptySet())
        }
    }

    fun copySelected() {
        val items = _storageState.value.selectedItems.toList()
        if (items.isNotEmpty()) {
            _clipboard.value = ClipboardState(items = items, isCut = false)
            clearSelection()
            showMessage("${items.size} item(s) copied")
        }
    }

    fun cutSelected() {
        val items = _storageState.value.selectedItems.toList()
        if (items.isNotEmpty()) {
            _clipboard.value = ClipboardState(items = items, isCut = true)
            clearSelection()
            showMessage("${items.size} item(s) cut to clipboard")
        }
    }

    fun copySingle(item: FileItem) {
        _clipboard.value = ClipboardState(items = listOf(item), isCut = false)
        showMessage("\"${item.name}\" copied")
    }

    fun cutSingle(item: FileItem) {
        _clipboard.value = ClipboardState(items = listOf(item), isCut = true)
        showMessage("\"${item.name}\" ready to move")
    }

    fun clearClipboard() {
        _clipboard.value = null
    }

    fun pasteToCurrentDirectory() {
        val clip = _clipboard.value ?: return
        val destDir = _storageState.value.currentDir

        viewModelScope.launch {
            if (clip.isCut) {
                val res = fileRepository.move(clip.items, destDir)
                if (res.isSuccess) {
                    showMessage("Moved ${res.getOrNull()} items successfully")
                    _clipboard.value = null
                } else {
                    showMessage("Move failed: ${res.exceptionOrNull()?.message}")
                }
            } else {
                val res = fileRepository.copy(clip.items, destDir)
                if (res.isSuccess) {
                    showMessage("Copied ${res.getOrNull()} items successfully")
                } else {
                    showMessage("Copy failed: ${res.exceptionOrNull()?.message}")
                }
            }
            refreshCurrentDirectory()
        }
    }

    fun createFolder(name: String) {
        viewModelScope.launch {
            val res = fileRepository.createFolder(_storageState.value.currentDir, name)
            if (res.isSuccess) {
                showMessage("Folder \"$name\" created")
                refreshCurrentDirectory()
            } else {
                showMessage("Error: ${res.exceptionOrNull()?.message}")
            }
        }
    }

    fun createTextFile(name: String) {
        viewModelScope.launch {
            val res = fileRepository.createTextFile(_storageState.value.currentDir, name)
            if (res.isSuccess) {
                showMessage("File \"$name\" created")
                refreshCurrentDirectory()
                openTextEditor(res.getOrThrow())
            } else {
                showMessage("Error: ${res.exceptionOrNull()?.message}")
            }
        }
    }

    fun deleteItems(items: List<FileItem>) {
        viewModelScope.launch {
            var count = 0
            for (item in items) {
                if (fileRepository.delete(item).getOrDefault(false)) count++
            }
            showMessage("Deleted $count item(s)")
            clearSelection()
            refreshCurrentDirectory()
        }
    }

    fun renameItem(item: FileItem, newName: String) {
        viewModelScope.launch {
            val res = fileRepository.rename(item, newName)
            if (res.isSuccess) {
                showMessage("Renamed to \"$newName\"")
                refreshCurrentDirectory()
            } else {
                showMessage("Rename failed: ${res.exceptionOrNull()?.message}")
            }
        }
    }

    fun zipItems(items: List<FileItem>, zipName: String) {
        val sanitized = if (zipName.endsWith(".zip")) zipName else "$zipName.zip"
        val zipFile = File(_storageState.value.currentDir, sanitized)

        viewModelScope.launch {
            val res = fileRepository.zip(items, zipFile)
            if (res.isSuccess) {
                showMessage("Archive \"$sanitized\" created")
                refreshCurrentDirectory()
            } else {
                showMessage("Failed to compress: ${res.exceptionOrNull()?.message}")
            }
        }
    }

    fun unzipItem(item: FileItem) {
        val outputName = item.name.removeSuffix(".zip").removeSuffix(".ZIP")
        val outputDir = File(_storageState.value.currentDir, outputName)

        viewModelScope.launch {
            val res = fileRepository.unzip(item.file, outputDir)
            if (res.isSuccess) {
                showMessage("Extracted to \"$outputName\"")
                refreshCurrentDirectory()
            } else {
                showMessage("Extraction failed: ${res.exceptionOrNull()?.message}")
            }
        }
    }

    // Cleaner Tools
    fun openCleaner() {
        navigateToScreen(Screen.CLEANER)
        startCleanScan()
    }

    fun startCleanScan() {
        viewModelScope.launch {
            isCleanScanning.value = true
            cleanedBytes.value = null
            val result = fileRepository.scanForClean()
            _cleanScan.value = result
            isCleanScanning.value = false
        }
    }

    fun performClean() {
        val scan = _cleanScan.value ?: return
        viewModelScope.launch {
            isCleaning.value = true
            var bytesCleaned = 0L
            for (item in scan.junkFiles) {
                val size = item.size
                if (fileRepository.delete(item).getOrDefault(false)) {
                    bytesCleaned += size
                }
            }
            cleanedBytes.value = bytesCleaned
            _cleanScan.value = scan.copy(junkFiles = emptyList())
            isCleaning.value = false
            refreshStorage()
            showMessage("Cleaned ${FileItem.formatBytes(bytesCleaned)} of junk files")
        }
    }

    // FTP Server Tool
    fun openFtpServer() {
        navigateToScreen(Screen.FTP_SERVER)
    }

    fun toggleFtpServer() {
        _ftpServerState.update { it.copy(isRunning = !it.isRunning) }
        val running = _ftpServerState.value.isRunning
        showMessage(if (running) "FTP Service started" else "FTP Service stopped")
    }

    // Category Screen
    fun openCategory(category: FileCategory, title: String) {
        if (title == "Downloads") {
            _selectedTab.value = MiTab.STORAGE
            loadDirectory(fileRepository.downloadsDirectory, addToHistory = true)
            _currentScreen.value = Screen.MAIN
            return
        }

        _categoryViewState.value = CategoryViewState(
            category = category,
            title = title,
            isLoading = true
        )
        navigateToScreen(Screen.CATEGORY_VIEW)

        viewModelScope.launch {
            val items = fileRepository.getCategoryFiles(category)
            _categoryViewState.update { it.copy(items = items, isLoading = false) }
        }
    }

    // Text Editor
    fun openTextEditor(file: File) {
        viewModelScope.launch {
            val content = fileRepository.readText(file).getOrDefault("")
            _textEditorState.value = TextEditorState(
                file = file,
                content = content,
                originalContent = content
            )
            navigateToScreen(Screen.TEXT_EDITOR)
        }
    }

    fun updateEditorContent(newContent: String) {
        _textEditorState.update { it.copy(content = newContent) }
    }

    fun toggleEditorWordWrap() {
        _textEditorState.update { it.copy(wordWrap = !it.wordWrap) }
    }

    fun saveEditorFile() {
        val state = _textEditorState.value
        val file = state.file ?: return

        viewModelScope.launch {
            _textEditorState.update { it.copy(isSaving = true) }
            val res = fileRepository.writeText(file, state.content)
            if (res.isSuccess) {
                _textEditorState.update { it.copy(originalContent = it.content, isSaving = false) }
                showMessage("Saved successfully")
            } else {
                _textEditorState.update { it.copy(isSaving = false) }
                showMessage("Save error: ${res.exceptionOrNull()?.message}")
            }
        }
    }

    // Image Viewer
    fun openImageViewer(file: File, siblingItems: List<FileItem>) {
        val imageFiles = siblingItems.filter { it.category == FileCategory.IMAGE }
        val index = imageFiles.indexOfFirst { it.file.absolutePath == file.absolutePath }.coerceAtLeast(0)
        _imageViewerState.value = ImageViewerState(
            currentFile = file,
            imageList = imageFiles,
            currentIndex = index
        )
        navigateToScreen(Screen.IMAGE_VIEWER)
    }

    fun nextImage() {
        val state = _imageViewerState.value
        if (state.imageList.isNotEmpty() && state.currentIndex < state.imageList.size - 1) {
            val nextIdx = state.currentIndex + 1
            _imageViewerState.value = state.copy(
                currentIndex = nextIdx,
                currentFile = state.imageList[nextIdx].file
            )
        }
    }

    fun prevImage() {
        val state = _imageViewerState.value
        if (state.imageList.isNotEmpty() && state.currentIndex > 0) {
            val prevIdx = state.currentIndex - 1
            _imageViewerState.value = state.copy(
                currentIndex = prevIdx,
                currentFile = state.imageList[prevIdx].file
            )
        }
    }

    // App Manager
    fun openAppManager() {
        navigateToScreen(Screen.APP_MANAGER)
        loadApps()
    }

    fun loadApps() {
        viewModelScope.launch {
            isAppsLoading.value = true
            val apps = appsRepository.getInstalledApps(includeSystemApps = includeSystemApps.value)
            _installedApps.value = apps
            isAppsLoading.value = false
        }
    }

    fun toggleSystemApps() {
        includeSystemApps.update { !it }
        loadApps()
    }

    fun setAppsSearchQuery(q: String) {
        appsSearchQuery.value = q
    }

    // AMOLED Mode
    fun toggleAmoledMode() {
        val next = !isAmoledMode.value
        isAmoledMode.value = next
        showMessage(if (next) "AMOLED Pure Black ON" else "AMOLED Pure Black OFF")
    }

    // Vault Functions
    fun openVault() {
        isVaultPinSet.value = vaultRepository.isPinSet()
        navigateToScreen(Screen.VAULT)
    }

    fun setupVaultPin(pin: String, answer: String) {
        vaultRepository.setPin(pin, answer)
        isVaultPinSet.value = true
        isVaultUnlocked.value = true
        loadVaultFiles()
        showMessage("Vault PIN set successfully")
    }

    fun unlockVault(pin: String): Boolean {
        val valid = vaultRepository.verifyPin(pin)
        if (valid) {
            isVaultUnlocked.value = true
            loadVaultFiles()
        }
        return valid
    }

    fun lockVault() {
        isVaultUnlocked.value = false
        _vaultFiles.value = emptyList()
        showMessage("Vault locked")
    }

    fun loadVaultFiles() {
        viewModelScope.launch {
            isVaultLoading.value = true
            _vaultFiles.value = vaultRepository.getVaultFiles()
            isVaultLoading.value = false
        }
    }

    fun addFileToVault(item: FileItem) {
        viewModelScope.launch {
            val success = vaultRepository.addToVault(item.file)
            if (success) {
                showMessage("Moved \"${item.name}\" to Private Vault")
                loadDirectory(_storageState.value.currentDir)
                if (isVaultUnlocked.value) loadVaultFiles()
            } else {
                showMessage("Failed to move file to Vault")
            }
        }
    }

    fun restoreFileFromVault(item: FileItem) {
        viewModelScope.launch {
            val target = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS).let {
                File(it, "Restored")
            }
            val success = vaultRepository.restoreFromVault(item.file, target)
            if (success) {
                showMessage("Restored to Downloads/Restored")
                loadVaultFiles()
                loadDirectory(_storageState.value.currentDir)
            } else {
                showMessage("Failed to restore file")
            }
        }
    }

    fun deleteFileFromVault(item: FileItem) {
        viewModelScope.launch {
            val success = vaultRepository.deleteFromVault(item.file)
            if (success) {
                showMessage("Deleted from Vault")
                loadVaultFiles()
            }
        }
    }

    // Duplicate Finder Functions
    fun openDuplicateFinder() {
        navigateToScreen(Screen.DUPLICATES)
    }

    fun scanForDuplicates() {
        viewModelScope.launch {
            isDuplicateScanning.value = true
            val result = duplicateRepository.findDuplicates()
            _duplicateScanResult.value = result
            selectedDuplicateFiles.value = result.groups.flatMap { it.duplicates }.toSet()
            isDuplicateScanning.value = false
        }
    }

    fun toggleSelectDuplicate(file: FileItem) {
        val current = selectedDuplicateFiles.value.toMutableSet()
        if (current.contains(file)) current.remove(file) else current.add(file)
        selectedDuplicateFiles.value = current
    }

    fun selectAllDuplicateCopies() {
        val allCopies = _duplicateScanResult.value?.groups?.flatMap { it.duplicates }?.toSet() ?: emptySet()
        selectedDuplicateFiles.value = allCopies
    }

    fun clearSelectedDuplicates() {
        selectedDuplicateFiles.value = emptySet()
    }

    fun deleteSelectedDuplicates() {
        val toDelete = selectedDuplicateFiles.value.toList()
        if (toDelete.isEmpty()) return
        viewModelScope.launch {
            val deletedCount = duplicateRepository.deleteFiles(toDelete)
            val freedBytes = toDelete.sumOf { it.size }
            showMessage("Deleted $deletedCount duplicates (Freed ${FileItem.formatBytes(freedBytes)})")
            selectedDuplicateFiles.value = emptySet()
            scanForDuplicates()
            refreshStorage()
            loadDirectory(_storageState.value.currentDir)
        }
    }

    // Storage Analyzer Functions
    fun openStorageAnalyzer() {
        navigateToScreen(Screen.STORAGE_ANALYZER)
    }

    fun analyzeStorage() {
        viewModelScope.launch {
            isStorageAnalyzing.value = true
            _storageAnalysisResult.value = storageAnalyzerRepository.analyzeStorage()
            isStorageAnalyzing.value = false
        }
    }

    fun openDirectoryFromAnalyzer(dir: File) {
        loadDirectory(dir, addToHistory = true)
        _selectedTab.value = MiTab.STORAGE
        navigateToScreen(Screen.MAIN)
    }

    // Batch Rename Function
    fun batchRename(pairs: List<Pair<FileItem, String>>) {
        viewModelScope.launch {
            var successCount = 0
            for ((item, newName) in pairs) {
                if (item.name != newName) {
                    val target = File(item.file.parentFile, newName)
                    if (item.file.renameTo(target)) {
                        successCount++
                    }
                }
            }
            showMessage("Renamed $successCount files successfully")
            clearSelection()
            loadDirectory(_storageState.value.currentDir)
        }
    }

    // Zip Archive Functions
    fun openZipViewer(file: File) {
        viewModelScope.launch {
            _zipViewerState.value = ZipViewerState(isLoading = true)
            navigateToScreen(Screen.ZIP_VIEWER)
            val res = zipRepository.inspectZipFile(file)
            res.fold(
                onSuccess = { info ->
                    _zipViewerState.value = ZipViewerState(archiveInfo = info, isLoading = false)
                },
                onFailure = { err ->
                    _zipViewerState.value = ZipViewerState(isLoading = false, errorMessage = err.localizedMessage)
                }
            )
        }
    }

    fun openZipFromUri(uri: android.net.Uri, name: String) {
        viewModelScope.launch {
            _zipViewerState.value = ZipViewerState(isLoading = true)
            navigateToScreen(Screen.ZIP_VIEWER)
            val res = zipRepository.inspectZipUri(uri, name)
            res.fold(
                onSuccess = { info ->
                    _zipViewerState.value = ZipViewerState(archiveInfo = info, isLoading = false)
                },
                onFailure = { err ->
                    _zipViewerState.value = ZipViewerState(isLoading = false, errorMessage = err.localizedMessage)
                }
            )
        }
    }

    fun extractZipArchive(zipFile: File, targetDir: File, selectedEntries: Set<String>? = null) {
        viewModelScope.launch {
            isZipExtracting.value = true
            val res = zipRepository.extractArchive(zipFile, targetDir, selectedEntries)
            isZipExtracting.value = false
            res.fold(
                onSuccess = { count ->
                    showMessage("Extracted $count files to ${targetDir.name}")
                    loadDirectory(_storageState.value.currentDir)
                    refreshStorage()
                },
                onFailure = { err ->
                    showMessage("Extraction failed: ${err.localizedMessage}")
                }
            )
        }
    }

    fun compressFilesToZip(items: List<File>, destinationZip: File, compressionLevel: Int) {
        viewModelScope.launch {
            showMessage("Compressing ${items.size} items...")
            val res = zipRepository.compressFiles(items, destinationZip, compressionLevel)
            res.fold(
                onSuccess = { createdFile ->
                    showMessage("Created ${createdFile.name} (${FileItem.formatBytes(createdFile.length())})")
                    clearSelection()
                    loadDirectory(_storageState.value.currentDir)
                    refreshStorage()
                },
                onFailure = { err ->
                    showMessage("Compression failed: ${err.localizedMessage}")
                }
            )
        }
    }

    fun showMessage(msg: String) {
        _message.value = msg
    }

    fun clearMessage() {
        _message.value = null
    }
}
