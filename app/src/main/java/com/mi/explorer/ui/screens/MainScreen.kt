package com.mi.explorer.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mi.explorer.data.model.*
import com.mi.explorer.ui.components.*
import com.mi.explorer.ui.theme.MiOrange
import com.mi.explorer.ui.viewmodel.ExplorerViewModel
import com.mi.explorer.ui.viewmodel.Screen
import com.mi.explorer.ui.viewmodel.StorageTabState
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: ExplorerViewModel,
    modifier: Modifier = Modifier
) {
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val storageSpace by viewModel.storageSpace.collectAsStateWithLifecycle()
    val storageState by viewModel.storageState.collectAsStateWithLifecycle()
    val recentFiles by viewModel.recentFiles.collectAsStateWithLifecycle()
    val isRecentLoading by viewModel.isRecentLoading.collectAsStateWithLifecycle()
    val clipboardState by viewModel.clipboard.collectAsStateWithLifecycle()

    var isSearchActive by remember { mutableStateOf(false) }
    var recentFilter by remember { mutableStateOf("All") }

    // Dialog states
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var showCreateFileDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    var newFileName by remember { mutableStateOf("") }
    var renameTarget by remember { mutableStateOf<FileItem?>(null) }
    var renameNewName by remember { mutableStateOf("") }
    var deleteTargets by remember { mutableStateOf<List<FileItem>?>(null) }
    var detailsTarget by remember { mutableStateOf<FileItem?>(null) }
    var zipTargets by remember { mutableStateOf<List<FileItem>?>(null) }
    var zipArchiveName by remember { mutableStateOf("") }
    var showSortMenu by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.testTag("main_screen"),
        topBar = {
            if (isSearchActive) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = storageState.searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            placeholder = { Text("Search files & folders...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MiOrange) },
                            trailingIcon = {
                                IconButton(onClick = {
                                    viewModel.setSearchQuery("")
                                    isSearchActive = false
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = "Close search")
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("mi_search_field")
                        )
                    }
                }
            } else {
                MiTopHeader(
                    selectedTab = selectedTab,
                    onTabSelected = { viewModel.selectTab(it) },
                    onSearchClick = { isSearchActive = true },
                    onCleanerClick = { viewModel.openCleaner() },
                    onFtpClick = { viewModel.openFtpServer() }
                )
            }
        },
        bottomBar = {
            MiClipboardBar(
                clipboardState = clipboardState,
                onPaste = { viewModel.pasteToCurrentDirectory() },
                onClear = { viewModel.clearClipboard() }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                MiTab.RECENT -> {
                    // Recent Tab Content
                    RecentTabContent(
                        recentFiles = recentFiles,
                        isLoading = isRecentLoading,
                        activeFilter = recentFilter,
                        onFilterSelected = { recentFilter = it },
                        onOpenFile = { item -> handleOpenFile(item, recentFiles, viewModel) },
                        onMenuAction = { action, item ->
                            when (action) {
                                "copy" -> viewModel.copySingle(item)
                                "cut" -> viewModel.cutSingle(item)
                                "rename" -> {
                                    renameTarget = item
                                    renameNewName = item.name
                                }
                                "delete" -> deleteTargets = listOf(item)
                                "details" -> detailsTarget = item
                            }
                        },
                        onRefresh = { viewModel.loadRecentFiles() }
                    )
                }
                MiTab.STORAGE -> {
                    // Storage Tab Content (MIUI Categories & Folder Navigation)
                    StorageTabContent(
                        storageSpace = storageSpace,
                        storageState = storageState,
                        rootStorageDir = viewModel.fileRepository.rootStorageDirectory,
                        onCleanClick = { viewModel.openCleaner() },
                        onCategoryClick = { cat, title -> viewModel.openCategory(cat, title) },
                        onAppManagerClick = { viewModel.openAppManager() },
                        onNavigateTo = { viewModel.loadDirectory(it, addToHistory = true) },
                        onNavigateUp = {
                            val parent = storageState.currentDir.parentFile
                            if (parent != null && parent.canRead()) {
                                viewModel.loadDirectory(parent, addToHistory = true)
                            }
                        },
                        onOpenFile = { item -> handleOpenFile(item, storageState.items, viewModel) },
                        onToggleSelect = { viewModel.toggleSelectItem(it) },
                        onSelectAll = { viewModel.selectAll() },
                        onClearSelection = { viewModel.clearSelection() },
                        onCopySelected = { viewModel.copySelected() },
                        onCutSelected = { viewModel.cutSelected() },
                        onDeleteSelected = { deleteTargets = storageState.selectedItems.toList() },
                        onZipSelected = {
                            zipArchiveName = "${storageState.currentDir.name}.zip"
                            zipTargets = storageState.selectedItems.toList()
                        },
                        onNewFolder = {
                            newFolderName = ""
                            showCreateFolderDialog = true
                        },
                        onNewFile = {
                            newFileName = ""
                            showCreateFileDialog = true
                        },
                        onToggleViewMode = { viewModel.toggleViewMode() },
                        onShowSortMenu = { showSortMenu = true },
                        onMenuAction = { action, item ->
                            when (action) {
                                "copy" -> viewModel.copySingle(item)
                                "cut" -> viewModel.cutSingle(item)
                                "rename" -> {
                                    renameTarget = item
                                    renameNewName = item.name
                                }
                                "delete" -> deleteTargets = listOf(item)
                                "details" -> detailsTarget = item
                                "zip" -> {
                                    zipArchiveName = "${item.name}.zip"
                                    zipTargets = listOf(item)
                                }
                                "unzip" -> viewModel.unzipItem(item)
                            }
                        }
                    )
                }
            }
        }
    }

    // Dialogs
    if (showCreateFolderDialog) {
        AlertDialog(
            onDismissRequest = { showCreateFolderDialog = false },
            title = { Text("New Folder") },
            text = {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    label = { Text("Folder Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newFolderName.isNotBlank()) {
                            viewModel.createFolder(newFolderName)
                            showCreateFolderDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MiOrange)
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateFolderDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showCreateFileDialog) {
        AlertDialog(
            onDismissRequest = { showCreateFileDialog = false },
            title = { Text("New File") },
            text = {
                OutlinedTextField(
                    value = newFileName,
                    onValueChange = { newFileName = it },
                    label = { Text("File Name (e.g. note.txt)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newFileName.isNotBlank()) {
                            viewModel.createTextFile(newFileName)
                            showCreateFileDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MiOrange)
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateFileDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    renameTarget?.let { item ->
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("Rename") },
            text = {
                OutlinedTextField(
                    value = renameNewName,
                    onValueChange = { renameNewName = it },
                    label = { Text("New name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameNewName.isNotBlank() && renameNewName != item.name) {
                            viewModel.renameItem(item, renameNewName)
                            renameTarget = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MiOrange)
                ) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    deleteTargets?.let { targets ->
        AlertDialog(
            onDismissRequest = { deleteTargets = null },
            title = { Text("Delete") },
            text = {
                Text("Delete ${targets.size} item(s)? This action cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteItems(targets)
                        deleteTargets = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTargets = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    detailsTarget?.let { item ->
        AlertDialog(
            onDismissRequest = { detailsTarget = null },
            title = { Text("Details") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "Name: ${item.name}", style = MaterialTheme.typography.bodyMedium)
                    Text(text = "Location: ${item.path}", style = MaterialTheme.typography.bodySmall)
                    Text(text = "Size: ${item.formattedSize}", style = MaterialTheme.typography.bodyMedium)
                    Text(text = "Type: ${item.category.name}", style = MaterialTheme.typography.bodyMedium)
                    Text(text = "Modified: ${item.formattedDate}", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = { detailsTarget = null }) {
                    Text("OK")
                }
            }
        )
    }

    zipTargets?.let { targets ->
        AlertDialog(
            onDismissRequest = { zipTargets = null },
            title = { Text("Compress to ZIP") },
            text = {
                OutlinedTextField(
                    value = zipArchiveName,
                    onValueChange = { zipArchiveName = it },
                    label = { Text("Archive Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (zipArchiveName.isNotBlank()) {
                            viewModel.zipItems(targets, zipArchiveName)
                            zipTargets = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MiOrange)
                ) {
                    Text("Compress")
                }
            },
            dismissButton = {
                TextButton(onClick = { zipTargets = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showSortMenu) {
        AlertDialog(
            onDismissRequest = { showSortMenu = false },
            title = { Text("Sort files") },
            text = {
                Column {
                    SortOptionRow("Name (A to Z)") {
                        viewModel.setSortType(SortType.NAME_ASC)
                        showSortMenu = false
                    }
                    SortOptionRow("Name (Z to A)") {
                        viewModel.setSortType(SortType.NAME_DESC)
                        showSortMenu = false
                    }
                    SortOptionRow("Date (Newest first)") {
                        viewModel.setSortType(SortType.DATE_NEWEST)
                        showSortMenu = false
                    }
                    SortOptionRow("Date (Oldest first)") {
                        viewModel.setSortType(SortType.DATE_OLDEST)
                        showSortMenu = false
                    }
                    SortOptionRow("Size (Largest first)") {
                        viewModel.setSortType(SortType.SIZE_LARGEST)
                        showSortMenu = false
                    }
                    SortOptionRow("Type (Extension)") {
                        viewModel.setSortType(SortType.TYPE)
                        showSortMenu = false
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    SortOptionRow(if (storageState.showHidden) "Hide hidden files" else "Show hidden files") {
                        viewModel.toggleShowHidden()
                        showSortMenu = false
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSortMenu = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun SortOptionRow(text: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        color = Color.Transparent
    ) {
        Text(text = text, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun RecentTabContent(
    recentFiles: List<FileItem>,
    isLoading: Boolean,
    activeFilter: String,
    onFilterSelected: (String) -> Unit,
    onOpenFile: (FileItem) -> Unit,
    onMenuAction: (String, FileItem) -> Unit,
    onRefresh: () -> Unit
) {
    val filters = listOf("All", "Images", "Docs", "APKs", "Archives", "Music")

    val filteredList = remember(recentFiles, activeFilter) {
        when (activeFilter) {
            "Images" -> recentFiles.filter { it.category == FileCategory.IMAGE }
            "Docs" -> recentFiles.filter { it.category == FileCategory.DOCUMENT }
            "APKs" -> recentFiles.filter { it.category == FileCategory.APK }
            "Archives" -> recentFiles.filter { it.category == FileCategory.ARCHIVE }
            "Music" -> recentFiles.filter { it.category == FileCategory.AUDIO }
            else -> recentFiles
        }
    }

    val grouped = remember(filteredList) {
        filteredList.groupBy { it.timeGroup }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("recent_tab_list"),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Filter chips bar
        item {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filters) { f ->
                    val isSelected = f == activeFilter
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onFilterSelected(f) }
                            .testTag("filter_chip_$f"),
                        color = if (isSelected) MiOrange else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = f,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                        )
                    }
                }
            }
        }

        if (isLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MiOrange)
                }
            }
        } else if (filteredList.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(64.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No recent files found",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            grouped.forEach { (timeHeader, files) ->
                item {
                    Text(
                        text = timeHeader,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 6.dp)
                    )
                }

                items(files, key = { it.path }) { file ->
                    MiFileRow(
                        item = file,
                        isSelected = false,
                        isSelectionMode = false,
                        onClick = { onOpenFile(file) },
                        onLongClick = {},
                        onToggleSelect = {},
                        onMenuAction = { onMenuAction(it, file) },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun StorageTabContent(
    storageSpace: StorageSpace,
    storageState: StorageTabState,
    rootStorageDir: File,
    onCleanClick: () -> Unit,
    onCategoryClick: (FileCategory, String) -> Unit,
    onAppManagerClick: () -> Unit,
    onNavigateTo: (File) -> Unit,
    onNavigateUp: () -> Unit,
    onOpenFile: (FileItem) -> Unit,
    onToggleSelect: (FileItem) -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onCopySelected: () -> Unit,
    onCutSelected: () -> Unit,
    onDeleteSelected: () -> Unit,
    onZipSelected: () -> Unit,
    onNewFolder: () -> Unit,
    onNewFile: () -> Unit,
    onToggleViewMode: () -> Unit,
    onShowSortMenu: () -> Unit,
    onMenuAction: (String, FileItem) -> Unit
) {
    val isRoot = storageState.currentDir == rootStorageDir

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("storage_tab_list"),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // Storage Card (Only on root storage)
        if (isRoot) {
            item {
                StorageCard(
                    storageSpace = storageSpace,
                    onCleanClick = onCleanClick,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // 8 Category Tiles Grid
            item {
                CategoryGrid(
                    onCategoryClick = onCategoryClick,
                    onCleanerClick = onCleanClick,
                    onAppManagerClick = onAppManagerClick,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }

            item {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }

        // Folder Path Breadcrumbs
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isRoot) {
                    IconButton(
                        onClick = onNavigateUp,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("navigate_up_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Up",
                            tint = MiOrange
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }

                MiBreadcrumbs(
                    currentDir = storageState.currentDir,
                    rootStorageDir = rootStorageDir,
                    onNavigateTo = onNavigateTo,
                    modifier = Modifier.weight(1f)
                )

                IconButton(onClick = onToggleViewMode, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = if (storageState.viewMode == ViewMode.LIST) Icons.Default.GridView else Icons.Default.ViewList,
                        contentDescription = "View Mode"
                    )
                }

                IconButton(onClick = onShowSortMenu, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Sort, contentDescription = "Sort")
                }
            }
        }

        // Action Toolbar (New Folder, New File, Select All)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (storageState.isSelectionMode) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${storageState.selectedItems.size} selected",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MiOrange)
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onCopySelected, modifier = Modifier.size(34.dp)) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                        }
                        IconButton(onClick = onCutSelected, modifier = Modifier.size(34.dp)) {
                            Icon(Icons.Default.ContentCut, contentDescription = "Cut")
                        }
                        IconButton(onClick = onZipSelected, modifier = Modifier.size(34.dp)) {
                            Icon(Icons.Default.Archive, contentDescription = "Zip")
                        }
                        IconButton(onClick = onDeleteSelected, modifier = Modifier.size(34.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444))
                        }
                        IconButton(onClick = onClearSelection, modifier = Modifier.size(34.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FilledTonalButton(
                            onClick = onNewFolder,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(32.dp).testTag("action_new_folder")
                        ) {
                            Icon(Icons.Default.CreateNewFolder, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New Folder", style = MaterialTheme.typography.labelSmall)
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        FilledTonalButton(
                            onClick = onNewFile,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(32.dp).testTag("action_new_file")
                        ) {
                            Icon(Icons.Default.NoteAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New File", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    TextButton(onClick = onSelectAll) {
                        Text("Select All", style = MaterialTheme.typography.labelMedium, color = MiOrange)
                    }
                }
            }
        }

        // Folder files listing
        if (storageState.isLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MiOrange)
                }
            }
        } else if (storageState.items.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "This folder is empty",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(storageState.items, key = { it.path }) { item ->
                val isSelected = storageState.selectedItems.contains(item)
                MiFileRow(
                    item = item,
                    isSelected = isSelected,
                    isSelectionMode = storageState.isSelectionMode,
                    onClick = {
                        if (item.isDirectory) {
                            onNavigateTo(item.file)
                        } else {
                            onOpenFile(item)
                        }
                    },
                    onLongClick = { onToggleSelect(item) },
                    onToggleSelect = { onToggleSelect(item) },
                    onMenuAction = { onMenuAction(it, item) },
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }
    }
}

fun handleOpenFile(
    item: FileItem,
    siblingItems: List<FileItem>,
    viewModel: ExplorerViewModel
) {
    when (item.category) {
        FileCategory.CODE, FileCategory.DOCUMENT -> {
            if (item.extension in listOf("txt", "md", "json", "xml", "kt", "java", "py", "sh", "html", "css", "js", "log", "csv")) {
                viewModel.openTextEditor(item.file)
            } else {
                viewModel.showMessage("Opening ${item.name}...")
            }
        }
        FileCategory.IMAGE -> {
            viewModel.openImageViewer(item.file, siblingItems)
        }
        FileCategory.ARCHIVE -> {
            viewModel.unzipItem(item)
        }
        FileCategory.APK -> {
            viewModel.openAppManager()
        }
        else -> {
            viewModel.openTextEditor(item.file)
        }
    }
}
