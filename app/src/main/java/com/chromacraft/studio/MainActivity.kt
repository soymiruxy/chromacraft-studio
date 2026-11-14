package com.chromacraft.studio

import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.chromacraft.studio.ui.adaptive.AdaptiveEditorLayout
import com.chromacraft.studio.ui.screens.HomeScreen
import com.chromacraft.studio.ui.theme.ChromaCraftStudioTheme
import com.chromacraft.studio.viewmodel.EditorIntent
import com.chromacraft.studio.viewmodel.EditorViewModel
import com.chromacraft.studio.viewmodel.ScreenType

/**
 * Actividad principal de ChromaCraft Studio.
 * Gestiona el ciclo de vida de la ventana, la navegación fluida entre Inicio y Editor,
 * el selector nativo de fotos (Photo Picker) y los intents externos.
 */
class MainActivity : ComponentActivity() {

    private val viewModel: EditorViewModel by viewModels()

    // Selector visual nativo de medios (Android 13+ Photo Picker con compatibilidad hacia atrás)
    private val pickMediaLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.processIntent(EditorIntent.ImportImageFromUri(it))
        }
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleIncomingImageIntent()

        setContent {
            val widthSizeClass = calculateWindowSizeClass(this).widthSizeClass
            val uiState by viewModel.uiState.collectAsState()

            ChromaCraftStudioTheme {
                when (uiState.currentScreen) {
                    ScreenType.HOME -> {
                        HomeScreen(
                            recentProjects = uiState.recentProjects,
                            recoveryAvailable = uiState.autoSaveRecoveryAvailable,
                            onOpenGallery = {
                                pickMediaLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            onCreateNewCanvas = { name, w, h, transparent, bg ->
                                viewModel.processIntent(
                                    EditorIntent.CreateBlankProject(name, w, h, transparent, bg)
                                )
                            },
                            onOpenProject = { projectId ->
                                viewModel.processIntent(EditorIntent.LoadProject(projectId))
                            },
                            onDeleteProject = { projectId ->
                                viewModel.processIntent(EditorIntent.DeleteProject(projectId))
                            },
                            onRecoverSession = {
                                viewModel.processIntent(EditorIntent.RecoverAutoSaveSession)
                            },
                            onDiscardRecovery = {
                                viewModel.processIntent(EditorIntent.DiscardAutoSaveSession)
                            }
                        )
                    }

                    ScreenType.EDITOR -> {
                        AdaptiveEditorLayout(
                            widthSizeClass = widthSizeClass,
                            uiState = uiState,
                            onIntent = viewModel::processIntent
                        )
                    }
                }
            }
        }
    }

    private fun handleIncomingImageIntent() {
        if (intent?.action == android.content.Intent.ACTION_SEND &&
            intent.type?.startsWith("image/") == true
        ) {
            val imageUri = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(android.content.Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(android.content.Intent.EXTRA_STREAM)
            }

            imageUri?.let { uri ->
                viewModel.processIntent(EditorIntent.ImportImageFromUri(uri))
            }
        }
    }
}
