package com.mi.explorer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mi.explorer.ui.screens.*
import com.mi.explorer.ui.theme.MiExplorerTheme
import com.mi.explorer.ui.viewmodel.ExplorerViewModel
import com.mi.explorer.ui.viewmodel.Screen

class MainActivity : ComponentActivity() {

    private val viewModel: ExplorerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MiExplorerTheme {
                MiMainApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MiMainApp(viewModel: ExplorerViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    BackHandler(enabled = true) {
        viewModel.handleBackPress()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Crossfade(
            targetState = currentScreen,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            label = "MiScreenTransition"
        ) { screen ->
            when (screen) {
                Screen.MAIN -> MainScreen(viewModel = viewModel)
                Screen.CLEANER -> CleanerScreen(viewModel = viewModel)
                Screen.FTP_SERVER -> FtpServerScreen(viewModel = viewModel)
                Screen.CATEGORY_VIEW -> CategoryViewScreen(viewModel = viewModel)
                Screen.TEXT_EDITOR -> TextEditorScreen(viewModel = viewModel)
                Screen.IMAGE_VIEWER -> ImageViewerScreen(viewModel = viewModel)
                Screen.APP_MANAGER -> AppManagerScreen(viewModel = viewModel)
            }
        }
    }
}
