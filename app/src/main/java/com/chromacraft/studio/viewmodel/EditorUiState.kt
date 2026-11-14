package com.chromacraft.studio.viewmodel

import android.net.Uri
import androidx.compose.ui.geometry.Offset
import com.chromacraft.studio.core.graphics.ExportEngine
import com.chromacraft.studio.core.model.AdjustmentProperties
import com.chromacraft.studio.core.model.BrushProperties
import com.chromacraft.studio.core.model.CropRatio
import com.chromacraft.studio.core.model.CurveChannel
import com.chromacraft.studio.core.model.FilterPreset
import com.chromacraft.studio.core.model.Layer
import com.chromacraft.studio.core.model.TextProperties
import com.chromacraft.studio.core.model.ToneCurveProperties
import com.chromacraft.studio.core.model.ToolType
import com.chromacraft.studio.core.project.ProjectHeader

enum class ScreenType {
    HOME,
    EDITOR
}

/**
 * Estado inmutable reactivo del editor de ChromaCraft Studio.
 */
data class EditorUiState(
    val currentScreen: ScreenType = ScreenType.HOME,
    val activeProjectId: String = "project_default",
    val activeProjectName: String = "Mi Creación",
    val recentProjects: List<ProjectHeader> = emptyList(),
    val autoSaveRecoveryAvailable: Boolean = false,
    val isComparingOriginal: Boolean = false,
    val layers: List<Layer> = emptyList(),
    val selectedLayerId: String? = null,
    val activeTool: ToolType = ToolType.NONE,
    val cropRatio: CropRatio = CropRatio.FREE,
    val rotationStep: Int = 0,
    val straightenAngle: Float = 0f,
    val brushProperties: BrushProperties = BrushProperties(),
    val currentAdjustments: AdjustmentProperties = AdjustmentProperties(),
    val currentToneCurves: ToneCurveProperties = ToneCurveProperties(),
    val activeCurveChannel: CurveChannel = CurveChannel.RGB,
    val currentFilterPreset: FilterPreset = FilterPreset.NONE,
    val transparentBackground: Boolean = false,
    val selectedResolutionScale: ExportEngine.ResolutionScale = ExportEngine.ResolutionScale.ORIGINAL,
    val selectedExportFormat: ExportEngine.ExportFormat = ExportEngine.ExportFormat.PNG,
    val exportQuality: Int = 95,
    val isExportDialogOpen: Boolean = false,
    val isResizeDialogOpen: Boolean = false,
    val isProjectSaveSuccess: Boolean = false,
    val lassoPoints: List<Offset> = emptyList(),
    val viewportScale: Float = 1.0f,
    val viewportPanX: Float = 0f,
    val viewportPanY: Float = 0f,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val isExporting: Boolean = false,
    val exportSuccessUri: Uri? = null,
    val errorMessage: String? = null,
    val isTextEditorOpen: Boolean = false,
    val editingTextProperties: TextProperties = TextProperties(),
    val canvasWidth: Int = 1080,
    val canvasHeight: Int = 1080
) {
    val selectedLayer: Layer?
        get() = layers.find { it.id == selectedLayerId }
}
