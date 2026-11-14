package com.chromacraft.studio.ui.adaptive

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chromacraft.studio.core.model.ToolType
import com.chromacraft.studio.ui.components.BottomToolBar
import com.chromacraft.studio.ui.components.EditorCanvas
import com.chromacraft.studio.ui.components.ExportDialog
import com.chromacraft.studio.ui.components.LayerManagerContent
import com.chromacraft.studio.ui.components.ResizeDialog
import com.chromacraft.studio.ui.components.SideToolRail
import com.chromacraft.studio.ui.components.SubToolPalette
import com.chromacraft.studio.ui.components.TextEditorModal
import com.chromacraft.studio.ui.components.TopBar
import com.chromacraft.studio.ui.theme.StudioBlack
import com.chromacraft.studio.ui.theme.StudioDark
import com.chromacraft.studio.viewmodel.EditorIntent
import com.chromacraft.studio.viewmodel.EditorUiState

/**
 * Orquestador de interfaz adaptativa para ChromaCraft Studio.
 * Diseñado bajo Material You (Material 3):
 * - Teléfono (Compact): Ergonomía del pulgar, Bottom Sheet y barra inferior fluida.
 * - Tableta / Paisaje (Expanded): Experiencia de estudio tipo Procreate/Photoshop con riel izquierdo
 *   e inspector lateral derecho persistente sin obstruir el lienzo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdaptiveEditorLayout(
    widthSizeClass: WindowWidthSizeClass,
    uiState: EditorUiState,
    onIntent: (EditorIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    val isTabletOrExpanded = widthSizeClass != WindowWidthSizeClass.Compact
    val snackbarHostState = remember { SnackbarHostState() }
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Interceptar botón atrás para regresar a la pantalla de inicio
    BackHandler {
        onIntent(EditorIntent.NavigateToHome)
    }

    LaunchedEffect(uiState.exportSuccessUri, uiState.errorMessage, uiState.isProjectSaveSuccess) {
        if (uiState.exportSuccessUri != null) {
            snackbarHostState.showSnackbar("¡Composición guardada con éxito en la Galería!")
            onIntent(EditorIntent.DismissMessage)
        }
        if (uiState.isProjectSaveSuccess) {
            snackbarHostState.showSnackbar("¡Proyecto editable guardado (.chroma)!")
            onIntent(EditorIntent.DismissMessage)
        }
        if (uiState.errorMessage != null) {
            snackbarHostState.showSnackbar("Error: ${uiState.errorMessage}")
            onIntent(EditorIntent.DismissMessage)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(StudioBlack)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 1. Barra de Utilidades Superior
            TopBar(
                uiState = uiState,
                onIntent = onIntent,
                onNavigateHome = { onIntent(EditorIntent.NavigateToHome) }
            )

            // 2. Disposición responsiva
            if (isTabletOrExpanded) {
                TabletLayout(
                    uiState = uiState,
                    onIntent = onIntent,
                    modifier = Modifier.weight(1f)
                )
            } else {
                PhoneLayout(
                    uiState = uiState,
                    onIntent = onIntent,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Modal Bottom Sheet de Capas para teléfonos
        if (!isTabletOrExpanded && uiState.activeTool == ToolType.LAYERS) {
            ModalBottomSheet(
                onDismissRequest = { onIntent(EditorIntent.CloseTool) },
                sheetState = bottomSheetState,
                containerColor = StudioDark
            ) {
                LayerManagerContent(
                    uiState = uiState,
                    onIntent = onIntent
                )
            }
        }

        // Diálogo Tipográfico
        if (uiState.isTextEditorOpen) {
            TextEditorModal(
                initialProperties = uiState.editingTextProperties,
                onIntent = onIntent
            )
        }

        // Diálogo de Exportación Avanzada
        if (uiState.isExportDialogOpen) {
            ExportDialog(
                uiState = uiState,
                onIntent = onIntent
            )
        }

        // Diálogo de Redimensión de Lienzo
        if (uiState.isResizeDialogOpen) {
            ResizeDialog(
                uiState = uiState,
                onIntent = onIntent
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun PhoneLayout(
    uiState: EditorUiState,
    onIntent: (EditorIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        EditorCanvas(
            uiState = uiState,
            onIntent = onIntent,
            modifier = Modifier.weight(1f)
        )

        AnimatedVisibility(
            visible = uiState.activeTool != ToolType.NONE && uiState.activeTool != ToolType.LAYERS,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            SubToolPalette(
                uiState = uiState,
                onIntent = onIntent
            )
        }

        BottomToolBar(
            activeTool = uiState.activeTool,
            onIntent = onIntent
        )
    }
}

@Composable
private fun TabletLayout(
    uiState: EditorUiState,
    onIntent: (EditorIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxSize()) {
        SideToolRail(
            activeTool = uiState.activeTool,
            onIntent = onIntent
        )

        EditorCanvas(
            uiState = uiState,
            onIntent = onIntent,
            modifier = Modifier.weight(1f)
        )

        Column(
            modifier = Modifier
                .width(340.dp)
                .fillMaxHeight()
                .background(StudioDark)
        ) {
            if (uiState.activeTool != ToolType.NONE && uiState.activeTool != ToolType.LAYERS) {
                SubToolPalette(
                    uiState = uiState,
                    onIntent = onIntent
                )
            }

            LayerManagerContent(
                uiState = uiState,
                onIntent = onIntent,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
