package com.mi.explorer.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mi.explorer.data.model.FileCategory
import com.mi.explorer.ui.components.MiFileRow
import com.mi.explorer.ui.theme.MiOrange
import com.mi.explorer.ui.viewmodel.ExplorerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryViewScreen(
    viewModel: ExplorerViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.categoryViewState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.testTag("category_view_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = state.title, style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = "${state.items.size} files",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.handleBackPress() },
                        modifier = Modifier.testTag("category_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MiOrange)
                }
            } else if (state.items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No ${state.title} found",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(state.items, key = { it.path }) { item ->
                        MiFileRow(
                            item = item,
                            isSelected = false,
                            isSelectionMode = false,
                            onClick = {
                                if (item.category == FileCategory.IMAGE) {
                                    viewModel.openImageViewer(item.file, state.items)
                                } else if (item.category == FileCategory.DOCUMENT || item.category == FileCategory.CODE) {
                                    viewModel.openTextEditor(item.file)
                                } else {
                                    handleOpenFile(item, state.items, viewModel)
                                }
                            },
                            onLongClick = {},
                            onToggleSelect = {},
                            onMenuAction = { action ->
                                when (action) {
                                    "copy" -> viewModel.copySingle(item)
                                    "cut" -> viewModel.cutSingle(item)
                                    else -> {}
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
