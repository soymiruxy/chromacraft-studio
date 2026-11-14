package com.chromacraft.studio.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.chromacraft.studio.core.graphics.BitmapUtils
import com.chromacraft.studio.core.graphics.CutoutEngine
import com.chromacraft.studio.core.graphics.ExportEngine
import com.chromacraft.studio.core.graphics.HistoryEngine
import com.chromacraft.studio.core.model.DrawingLayer
import com.chromacraft.studio.core.model.ImageLayer
import com.chromacraft.studio.core.model.Layer
import com.chromacraft.studio.core.model.LayerMask
import com.chromacraft.studio.core.model.StickerLayer
import com.chromacraft.studio.core.model.TextLayer
import com.chromacraft.studio.core.model.ToolType
import com.chromacraft.studio.core.project.AutoSaveManager
import com.chromacraft.studio.core.project.ProjectRepository
import com.chromacraft.studio.core.project.ProjectSerializer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * ViewModel central de ChromaCraft Studio siguiendo el patrón MVI.
 * Controla el ciclo de vida de los datos, la pantalla de inicio, recuperación de sesión,
 * optimización de memoria de mapas de bits, guardado automático y exportación.
 */
class EditorViewModel(application: Application) : AndroidViewModel(application) {

    private val historyEngine = HistoryEngine(maxDepth = 30)
    private val projectRepository = ProjectRepository(application)
    private val autoSaveManager = AutoSaveManager(application, viewModelScope)

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    init {
        loadHomeData()
    }

    private fun loadHomeData() {
        viewModelScope.launch {
            val projects = projectRepository.listProjects()
            val hasRecovery = autoSaveManager.hasRecoverySession()
            _uiState.update {
                it.copy(
                    recentProjects = projects,
                    autoSaveRecoveryAvailable = hasRecovery
                )
            }
        }
    }

    fun processIntent(intent: EditorIntent) {
        when (intent) {
            is EditorIntent.NavigateToHome -> {
                // Guardar proyecto en segundo plano y refrescar lista
                saveCurrentProjectSilently()
                viewModelScope.launch {
                    val projects = projectRepository.listProjects()
                    _uiState.update { it.copy(currentScreen = ScreenType.HOME, recentProjects = projects) }
                }
            }

            is EditorIntent.NavigateToEditor -> {
                _uiState.update { it.copy(currentScreen = ScreenType.EDITOR) }
            }

            is EditorIntent.CreateBlankProject -> {
                val projectId = "project_${System.currentTimeMillis()}"
                val baseBitmap = Bitmap.createBitmap(intent.width, intent.height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(baseBitmap)
                if (intent.transparentBg) {
                    canvas.drawColor(Color.TRANSPARENT)
                } else {
                    val p = Paint().apply { color = intent.bgColor.toInt() }
                    canvas.drawRect(0f, 0f, intent.width.toFloat(), intent.height.toFloat(), p)
                }

                val baseLayer = ImageLayer(
                    name = "Fondo",
                    bitmap = baseBitmap,
                    intrinsicWidth = intent.width,
                    intrinsicHeight = intent.height
                )

                historyEngine.clear()
                _uiState.update {
                    it.copy(
                        currentScreen = ScreenType.EDITOR,
                        activeProjectId = projectId,
                        activeProjectName = intent.name,
                        canvasWidth = intent.width,
                        canvasHeight = intent.height,
                        transparentBackground = intent.transparentBg,
                        layers = listOf(baseLayer),
                        selectedLayerId = baseLayer.id,
                        activeTool = ToolType.NONE
                    )
                }
                syncHistoryState()
                triggerAutoSave()
            }

            is EditorIntent.ImportImageFromUri -> {
                viewModelScope.launch {
                    val result = BitmapUtils.decodeSampledBitmapFromUri(getApplication(), intent.uri)
                    result.onSuccess { bmp ->
                        val projectId = "project_${System.currentTimeMillis()}"
                        val layer = ImageLayer(
                            name = "Foto Original",
                            bitmap = bmp,
                            intrinsicWidth = bmp.width,
                            intrinsicHeight = bmp.height
                        )
                        historyEngine.clear()
                        _uiState.update {
                            it.copy(
                                currentScreen = ScreenType.EDITOR,
                                activeProjectId = projectId,
                                activeProjectName = "Foto_${System.currentTimeMillis() % 10000}",
                                canvasWidth = bmp.width,
                                canvasHeight = bmp.height,
                                transparentBackground = false,
                                layers = listOf(layer),
                                selectedLayerId = layer.id,
                                activeTool = ToolType.NONE
                            )
                        }
                        syncHistoryState()
                        triggerAutoSave()
                    }.onFailure { err ->
                        _uiState.update { it.copy(errorMessage = "Error al abrir imagen: ${err.message}") }
                    }
                }
            }

            is EditorIntent.LoadProject -> {
                // En un proyecto real leería el archivo .chroma y recrearía las capas
                _uiState.update { it.copy(currentScreen = ScreenType.EDITOR) }
            }

            is EditorIntent.DeleteProject -> {
                viewModelScope.launch {
                    projectRepository.deleteProject(intent.projectId)
                    val updated = projectRepository.listProjects()
                    _uiState.update { it.copy(recentProjects = updated) }
                }
            }

            is EditorIntent.RecoverAutoSaveSession -> {
                viewModelScope.launch {
                    val json = autoSaveManager.readRecoverySession()
                    if (json != null) {
                        _uiState.update {
                            it.copy(
                                currentScreen = ScreenType.EDITOR,
                                autoSaveRecoveryAvailable = false
                            )
                        }
                    }
                }
            }

            is EditorIntent.DiscardAutoSaveSession -> {
                viewModelScope.launch {
                    autoSaveManager.clearRecoverySession()
                    _uiState.update { it.copy(autoSaveRecoveryAvailable = false) }
                }
            }

            is EditorIntent.SetComparingOriginal -> {
                _uiState.update { it.copy(isComparingOriginal = intent.comparing) }
            }

            is EditorIntent.SelectTool -> {
                _uiState.update { it.copy(activeTool = intent.tool) }
                if (intent.tool == ToolType.DRAW) {
                    ensureDrawingLayerActive()
                }
            }

            is EditorIntent.CloseTool -> {
                _uiState.update { it.copy(activeTool = ToolType.NONE, lassoPoints = emptyList()) }
            }

            is EditorIntent.SelectLayer -> {
                _uiState.update { it.copy(selectedLayerId = intent.layerId) }
            }

            is EditorIntent.AddImageLayer -> {
                saveHistorySnapshot("Añadir Imagen")
                val layer = ImageLayer(
                    name = intent.name,
                    bitmap = intent.bitmap,
                    intrinsicWidth = intent.bitmap.width,
                    intrinsicHeight = intent.bitmap.height
                )
                _uiState.update { state ->
                    state.copy(
                        layers = state.layers + layer,
                        selectedLayerId = layer.id
                    )
                }
                syncHistoryState()
                triggerAutoSave()
            }

            is EditorIntent.AddTextLayer -> {
                saveHistorySnapshot("Añadir Texto")
                val layer = TextLayer(
                    name = "Texto: ${intent.properties.text.take(8)}",
                    textProperties = intent.properties
                )
                _uiState.update { state ->
                    state.copy(
                        layers = state.layers + layer,
                        selectedLayerId = layer.id,
                        isTextEditorOpen = false
                    )
                }
                syncHistoryState()
                triggerAutoSave()
            }

            is EditorIntent.AddStickerLayer -> {
                saveHistorySnapshot("Añadir Forma")
                val layer = StickerLayer(
                    name = intent.properties.shapeType.label,
                    stickerProperties = intent.properties
                )
                _uiState.update { state ->
                    state.copy(
                        layers = state.layers + layer,
                        selectedLayerId = layer.id
                    )
                }
                syncHistoryState()
                triggerAutoSave()
            }

            is EditorIntent.AddDrawingLayer -> {
                saveHistorySnapshot("Nueva Capa de Dibujo")
                val layer = DrawingLayer(name = "Capa Dibujo")
                _uiState.update { state ->
                    state.copy(
                        layers = state.layers + layer,
                        selectedLayerId = layer.id
                    )
                }
                syncHistoryState()
                triggerAutoSave()
            }

            is EditorIntent.AddStroke -> {
                val state = _uiState.value
                val hasDrawingLayer = state.layers.any { it is DrawingLayer }
                val targetLayers = if (!hasDrawingLayer) {
                    val newDrawLayer = DrawingLayer(name = "Dibujo")
                    state.layers + newDrawLayer
                } else {
                    state.layers
                }
                val activeDrawingId = if (state.selectedLayer is DrawingLayer) {
                    state.selectedLayerId
                } else {
                    targetLayers.filterIsInstance<DrawingLayer>().last().id
                }
                val updated = targetLayers.map { layer ->
                    if (layer.id == activeDrawingId && layer is DrawingLayer) {
                        layer.copy(strokes = layer.strokes + intent.stroke)
                    } else layer
                }
                _uiState.update { it.copy(layers = updated, selectedLayerId = activeDrawingId) }
                triggerAutoSave()
            }

            is EditorIntent.UpdateLayerTransform -> {
                val layers = _uiState.value.layers.map { layer ->
                    if (layer.id == intent.layerId) {
                        layer.copyWithTransform(
                            translationX = intent.translationX,
                            translationY = intent.translationY,
                            scaleX = intent.scaleX,
                            scaleY = intent.scaleY,
                            rotationDegrees = intent.rotationDegrees
                        )
                    } else layer
                }
                _uiState.update { it.copy(layers = layers) }
                triggerAutoSave()
            }

            is EditorIntent.FlipLayerHorizontal -> {
                saveHistorySnapshot("Voltear Horizontal")
                val layers = _uiState.value.layers.map { layer ->
                    if (layer.id == intent.layerId) {
                        layer.copyWithTransform(flipHorizontal = !layer.flipHorizontal)
                    } else layer
                }
                _uiState.update { it.copy(layers = layers) }
                syncHistoryState()
                triggerAutoSave()
            }

            is EditorIntent.FlipLayerVertical -> {
                saveHistorySnapshot("Voltear Vertical")
                val layers = _uiState.value.layers.map { layer ->
                    if (layer.id == intent.layerId) {
                        layer.copyWithTransform(flipVertical = !layer.flipVertical)
                    } else layer
                }
                _uiState.update { it.copy(layers = layers) }
                syncHistoryState()
                triggerAutoSave()
            }

            is EditorIntent.Rotate90 -> {
                saveHistorySnapshot("Rotar 90°")
                val delta = if (intent.clockwise) 90f else -90f
                val selId = _uiState.value.selectedLayerId
                val layers = _uiState.value.layers.map { layer ->
                    if (layer.id == selId) {
                        layer.copyWithTransform(rotationDegrees = (layer.rotationDegrees + delta) % 360f)
                    } else layer
                }
                _uiState.update { it.copy(layers = layers) }
                syncHistoryState()
                triggerAutoSave()
            }

            is EditorIntent.SetStraightenAngle -> {
                _uiState.update { it.copy(straightenAngle = intent.angle) }
            }

            is EditorIntent.ResizeCanvas -> {
                saveHistorySnapshot("Redimensionar Lienzo")
                _uiState.update {
                    it.copy(
                        canvasWidth = intent.newWidth.coerceIn(200, 8192),
                        canvasHeight = intent.newHeight.coerceIn(200, 8192),
                        isResizeDialogOpen = false
                    )
                }
                syncHistoryState()
                triggerAutoSave()
            }

            is EditorIntent.ToggleTransparentBackground -> {
                _uiState.update { it.copy(transparentBackground = !it.transparentBackground) }
                triggerAutoSave()
            }

            is EditorIntent.UpdateLayerVisibility -> {
                saveHistorySnapshot("Visibilidad")
                val layers = _uiState.value.layers.map { layer ->
                    if (layer.id == intent.layerId) {
                        layer.copyWithVisibility(intent.isVisible)
                    } else layer
                }
                _uiState.update { it.copy(layers = layers) }
                syncHistoryState()
                triggerAutoSave()
            }

            is EditorIntent.UpdateLayerOpacity -> {
                val layers = _uiState.value.layers.map { layer ->
                    if (layer.id == intent.layerId) {
                        layer.copyWithOpacity(intent.opacity)
                    } else layer
                }
                _uiState.update { it.copy(layers = layers) }
                triggerAutoSave()
            }

            is EditorIntent.UpdateLayerBlendMode -> {
                saveHistorySnapshot("Modo Fusión")
                val layers = _uiState.value.layers.map { layer ->
                    if (layer.id == intent.layerId) {
                        layer.copyWithBlendMode(intent.blendMode)
                    } else layer
                }
                _uiState.update { it.copy(layers = layers) }
                syncHistoryState()
                triggerAutoSave()
            }

            is EditorIntent.ToggleLayerMask -> {
                saveHistorySnapshot("Alternar Máscara")
                val layers = _uiState.value.layers.map { layer ->
                    if (layer.id == intent.layerId) {
                        val m = layer.mask
                        if (m != null) layer.copyWithMask(m.copy(isEnabled = !m.isEnabled)) else layer
                    } else layer
                }
                _uiState.update { it.copy(layers = layers) }
                syncHistoryState()
                triggerAutoSave()
            }

            is EditorIntent.InvertLayerMask -> {
                saveHistorySnapshot("Invertir Máscara")
                val layers = _uiState.value.layers.map { layer ->
                    if (layer.id == intent.layerId) {
                        val m = layer.mask
                        if (m != null) layer.copyWithMask(m.copy(isInverted = !m.isInverted)) else layer
                    } else layer
                }
                _uiState.update { it.copy(layers = layers) }
                syncHistoryState()
                triggerAutoSave()
            }

            is EditorIntent.ReorderLayers -> {
                saveHistorySnapshot("Reordenar Capas")
                val current = _uiState.value.layers.toMutableList()
                if (intent.fromIndex in current.indices && intent.toIndex in current.indices) {
                    val item = current.removeAt(intent.fromIndex)
                    current.add(intent.toIndex, item)
                    _uiState.update { it.copy(layers = current) }
                    syncHistoryState()
                    triggerAutoSave()
                }
            }

            is EditorIntent.DeleteLayer -> {
                saveHistorySnapshot("Eliminar Capa")
                val layers = _uiState.value.layers.filterNot { it.id == intent.layerId }
                _uiState.update { state ->
                    state.copy(
                        layers = layers,
                        selectedLayerId = if (state.selectedLayerId == intent.layerId) layers.lastOrNull()?.id else state.selectedLayerId
                    )
                }
                syncHistoryState()
                triggerAutoSave()
            }

            is EditorIntent.DuplicateLayer -> {
                saveHistorySnapshot("Duplicar Capa")
                val target = _uiState.value.layers.find { it.id == intent.layerId } ?: return
                val cloned = when (target) {
                    is ImageLayer -> target.copy(id = UUID.randomUUID().toString(), name = "${target.name} (Copia)", translationX = target.translationX + 40f, translationY = target.translationY + 40f)
                    is DrawingLayer -> target.copy(id = UUID.randomUUID().toString(), name = "${target.name} (Copia)", translationX = target.translationX + 40f, translationY = target.translationY + 40f)
                    is TextLayer -> target.copy(id = UUID.randomUUID().toString(), name = "${target.name} (Copia)", translationX = target.translationX + 40f, translationY = target.translationY + 40f)
                    is StickerLayer -> target.copy(id = UUID.randomUUID().toString(), name = "${target.name} (Copia)", translationX = target.translationX + 40f, translationY = target.translationY + 40f)
                }
                _uiState.update { state ->
                    state.copy(
                        layers = state.layers + cloned,
                        selectedLayerId = cloned.id
                    )
                }
                syncHistoryState()
                triggerAutoSave()
            }

            is EditorIntent.UpdateAdjustments -> {
                _uiState.update { state ->
                    val updatedLayers = state.layers.map { layer ->
                        if (layer.id == state.selectedLayerId && layer is ImageLayer) {
                            layer.copy(adjustments = intent.adjustments)
                        } else layer
                    }
                    state.copy(
                        layers = updatedLayers,
                        currentAdjustments = intent.adjustments
                    )
                }
                triggerAutoSave()
            }

            is EditorIntent.UpdateToneCurves -> {
                _uiState.update { state ->
                    val updatedLayers = state.layers.map { layer ->
                        if (layer.id == state.selectedLayerId && layer is ImageLayer) {
                            layer.copy(curves = intent.curves)
                        } else layer
                    }
                    state.copy(
                        layers = updatedLayers,
                        currentToneCurves = intent.curves
                    )
                }
                triggerAutoSave()
            }

            is EditorIntent.SetActiveCurveChannel -> {
                _uiState.update { it.copy(activeCurveChannel = intent.channel) }
            }

            is EditorIntent.SelectFilterPreset -> {
                saveHistorySnapshot("Filtro: ${intent.preset.displayName}")
                _uiState.update { state ->
                    val updatedLayers = state.layers.map { layer ->
                        if (layer.id == state.selectedLayerId && layer is ImageLayer) {
                            layer.copy(filterPreset = intent.preset)
                        } else layer
                    }
                    state.copy(
                        layers = updatedLayers,
                        currentFilterPreset = intent.preset
                    )
                }
                syncHistoryState()
                triggerAutoSave()
            }

            is EditorIntent.UpdateBrushProperties -> {
                _uiState.update { it.copy(brushProperties = intent.properties) }
            }

            is EditorIntent.SetCropRatio -> {
                _uiState.update { it.copy(cropRatio = intent.ratio) }
            }

            is EditorIntent.ApplyCrop -> {
                saveHistorySnapshot("Recortar")
                val newW = intent.cropRect.width().toInt().coerceAtLeast(100)
                val newH = intent.cropRect.height().toInt().coerceAtLeast(100)
                _uiState.update {
                    it.copy(
                        canvasWidth = newW,
                        canvasHeight = newH,
                        activeTool = ToolType.NONE
                    )
                }
                syncHistoryState()
                triggerAutoSave()
            }

            is EditorIntent.AddLassoPoint -> {
                _uiState.update { it.copy(lassoPoints = it.lassoPoints + intent.offset) }
            }

            is EditorIntent.FinishLassoCutout -> {
                val points = _uiState.value.lassoPoints
                val selId = _uiState.value.selectedLayerId
                if (points.size >= 3 && selId != null) {
                    saveHistorySnapshot("Recorte Lazo")
                    val mask = LayerMask(lassoPoints = points, isInverted = intent.invert, isEnabled = true)
                    val layers = _uiState.value.layers.map { layer ->
                        if (layer.id == selId) layer.copyWithMask(mask) else layer
                    }
                    _uiState.update { it.copy(layers = layers, lassoPoints = emptyList(), activeTool = ToolType.NONE) }
                    syncHistoryState()
                    triggerAutoSave()
                }
            }

            is EditorIntent.ClearLasso -> {
                _uiState.update { it.copy(lassoPoints = emptyList()) }
            }

            is EditorIntent.RemoveColorBackground -> {
                val sel = _uiState.value.selectedLayer
                if (sel is ImageLayer && sel.bitmap != null) {
                    saveHistorySnapshot("Eliminar Fondo")
                    val processedBmp = CutoutEngine.removeColorBackground(sel.bitmap, intent.targetColor, intent.tolerance)
                    val layers = _uiState.value.layers.map { layer ->
                        if (layer.id == sel.id) (layer as ImageLayer).copy(bitmap = processedBmp) else layer
                    }
                    _uiState.update { it.copy(layers = layers, activeTool = ToolType.NONE) }
                    syncHistoryState()
                    triggerAutoSave()
                }
            }

            is EditorIntent.OpenTextEditor -> {
                _uiState.update { it.copy(isTextEditorOpen = true, editingTextProperties = intent.initialProperties) }
            }

            is EditorIntent.CloseTextEditor -> {
                _uiState.update { it.copy(isTextEditorOpen = false) }
            }

            is EditorIntent.OpenExportDialog -> {
                _uiState.update { it.copy(isExportDialogOpen = true) }
            }

            is EditorIntent.CloseExportDialog -> {
                _uiState.update { it.copy(isExportDialogOpen = false) }
            }

            is EditorIntent.OpenResizeDialog -> {
                _uiState.update { it.copy(isResizeDialogOpen = true) }
            }

            is EditorIntent.CloseResizeDialog -> {
                _uiState.update { it.copy(isResizeDialogOpen = false) }
            }

            is EditorIntent.Undo -> {
                val current = getCurrentSnapshot()
                val previous = historyEngine.undo(current)
                if (previous != null) {
                    _uiState.update {
                        it.copy(
                            layers = previous.layers,
                            selectedLayerId = previous.selectedLayerId
                        )
                    }
                    syncHistoryState()
                    triggerAutoSave()
                }
            }

            is EditorIntent.Redo -> {
                val current = getCurrentSnapshot()
                val next = historyEngine.redo(current)
                if (next != null) {
                    _uiState.update {
                        it.copy(
                            layers = next.layers,
                            selectedLayerId = next.selectedLayerId
                        )
                    }
                    syncHistoryState()
                    triggerAutoSave()
                }
            }

            is EditorIntent.Export -> {
                _uiState.update { it.copy(isExportDialogOpen = false) }
                exportComposition(intent.format, intent.scale, intent.quality)
            }

            is EditorIntent.SaveProject -> {
                saveCurrentProject()
            }

            is EditorIntent.DismissMessage -> {
                _uiState.update { it.copy(errorMessage = null, exportSuccessUri = null, isProjectSaveSuccess = false) }
            }
        }
    }

    private fun ensureDrawingLayerActive() {
        val state = _uiState.value
        val active = state.selectedLayer
        if (active !is DrawingLayer) {
            val existingDrawing = state.layers.filterIsInstance<DrawingLayer>().lastOrNull()
            if (existingDrawing != null) {
                _uiState.update { it.copy(selectedLayerId = existingDrawing.id) }
            } else {
                processIntent(EditorIntent.AddDrawingLayer)
            }
        }
    }

    private fun getCurrentSnapshot(): HistoryEngine.HistorySnapshot {
        val state = _uiState.value
        return HistoryEngine.HistorySnapshot(
            layers = state.layers,
            selectedLayerId = state.selectedLayerId,
            description = "Estado"
        )
    }

    private fun saveHistorySnapshot(description: String) {
        historyEngine.pushSnapshot(getCurrentSnapshot().copy(description = description))
    }

    private fun syncHistoryState() {
        _uiState.update {
            it.copy(
                canUndo = historyEngine.canUndo,
                canRedo = historyEngine.canRedo
            )
        }
    }

    private fun triggerAutoSave() {
        val s = _uiState.value
        autoSaveManager.scheduleAutoSave(
            projectName = s.activeProjectName,
            width = s.canvasWidth,
            height = s.canvasHeight,
            transparentBg = s.transparentBackground,
            layers = s.layers
        )
    }

    private fun saveCurrentProjectSilently() {
        val s = _uiState.value
        viewModelScope.launch {
            projectRepository.saveProject(
                projectId = s.activeProjectId,
                name = s.activeProjectName,
                width = s.canvasWidth,
                height = s.canvasHeight,
                transparentBg = s.transparentBackground,
                layers = s.layers
            )
        }
    }

    private fun saveCurrentProject() {
        val s = _uiState.value
        viewModelScope.launch {
            val res = projectRepository.saveProject(
                projectId = s.activeProjectId,
                name = s.activeProjectName,
                width = s.canvasWidth,
                height = s.canvasHeight,
                transparentBg = s.transparentBackground,
                layers = s.layers
            )
            res.onSuccess {
                _uiState.update { it.copy(isProjectSaveSuccess = true) }
            }.onFailure { err ->
                _uiState.update { it.copy(errorMessage = "Error al guardar proyecto: ${err.message}") }
            }
        }
    }

    private fun exportComposition(
        format: ExportEngine.ExportFormat,
        scale: ExportEngine.ResolutionScale,
        quality: Int
    ) {
        val state = _uiState.value
        _uiState.update { it.copy(isExporting = true) }

        viewModelScope.launch {
            val result = ExportEngine.exportToGallery(
                context = getApplication(),
                layers = state.layers,
                baseWidth = state.canvasWidth,
                baseHeight = state.canvasHeight,
                format = format,
                scale = scale,
                quality = quality,
                transparentBackground = state.transparentBackground
            )

            result.onSuccess { uri ->
                _uiState.update { it.copy(isExporting = false, exportSuccessUri = uri) }
            }.onFailure { err ->
                _uiState.update { it.copy(isExporting = false, errorMessage = err.message) }
            }
        }
    }
}
