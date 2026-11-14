package com.chromacraft.studio.viewmodel

import android.graphics.Bitmap
import android.graphics.RectF
import android.net.Uri
import androidx.compose.ui.geometry.Offset
import com.chromacraft.studio.core.graphics.ExportEngine
import com.chromacraft.studio.core.model.AdjustmentProperties
import com.chromacraft.studio.core.model.BlendModeType
import com.chromacraft.studio.core.model.BrushProperties
import com.chromacraft.studio.core.model.CropRatio
import com.chromacraft.studio.core.model.CurveChannel
import com.chromacraft.studio.core.model.FilterPreset
import com.chromacraft.studio.core.model.StickerProperties
import com.chromacraft.studio.core.model.StrokePath
import com.chromacraft.studio.core.model.TextProperties
import com.chromacraft.studio.core.model.ToneCurveProperties
import com.chromacraft.studio.core.model.ToolType

/**
 * Acciones e intenciones unidireccionales generadas por la interacción del usuario.
 */
sealed interface EditorIntent {
    data object NavigateToHome : EditorIntent
    data object NavigateToEditor : EditorIntent
    data class CreateBlankProject(
        val name: String,
        val width: Int,
        val height: Int,
        val transparentBg: Boolean,
        val bgColor: Long
    ) : EditorIntent
    data class ImportImageFromUri(val uri: Uri) : EditorIntent
    data class LoadProject(val projectId: String) : EditorIntent
    data class DeleteProject(val projectId: String) : EditorIntent
    data object RecoverAutoSaveSession : EditorIntent
    data object DiscardAutoSaveSession : EditorIntent

    data class SetComparingOriginal(val comparing: Boolean) : EditorIntent

    data class SelectTool(val tool: ToolType) : EditorIntent
    data object CloseTool : EditorIntent

    data class SelectLayer(val layerId: String?) : EditorIntent
    data class AddImageLayer(val bitmap: Bitmap, val name: String = "Foto") : EditorIntent
    data class AddTextLayer(val properties: TextProperties) : EditorIntent
    data class AddStickerLayer(val properties: StickerProperties) : EditorIntent
    data object AddDrawingLayer : EditorIntent

    data class AddStroke(val stroke: StrokePath) : EditorIntent
    data class UpdateLayerTransform(
        val layerId: String,
        val translationX: Float,
        val translationY: Float,
        val scaleX: Float,
        val scaleY: Float,
        val rotationDegrees: Float
    ) : EditorIntent

    data class FlipLayerHorizontal(val layerId: String) : EditorIntent
    data class FlipLayerVertical(val layerId: String) : EditorIntent
    data class Rotate90(val clockwise: Boolean = true) : EditorIntent
    data class SetStraightenAngle(val angle: Float) : EditorIntent
    data class ResizeCanvas(val newWidth: Int, val newHeight: Int) : EditorIntent
    data object ToggleTransparentBackground : EditorIntent

    data class UpdateLayerVisibility(val layerId: String, val isVisible: Boolean) : EditorIntent
    data class UpdateLayerOpacity(val layerId: String, val opacity: Float) : EditorIntent
    data class UpdateLayerBlendMode(val layerId: String, val blendMode: BlendModeType) : EditorIntent
    data class ToggleLayerMask(val layerId: String) : EditorIntent
    data class InvertLayerMask(val layerId: String) : EditorIntent
    data class ReorderLayers(val fromIndex: Int, val toIndex: Int) : EditorIntent
    data class DeleteLayer(val layerId: String) : EditorIntent
    data class DuplicateLayer(val layerId: String) : EditorIntent

    data class UpdateAdjustments(val adjustments: AdjustmentProperties) : EditorIntent
    data class UpdateToneCurves(val curves: ToneCurveProperties) : EditorIntent
    data class SetActiveCurveChannel(val channel: CurveChannel) : EditorIntent
    data class SelectFilterPreset(val preset: FilterPreset) : EditorIntent
    data class UpdateBrushProperties(val properties: BrushProperties) : EditorIntent
    data class SetCropRatio(val ratio: CropRatio) : EditorIntent
    data class ApplyCrop(val cropRect: RectF) : EditorIntent

    data class AddLassoPoint(val offset: Offset) : EditorIntent
    data class FinishLassoCutout(val invert: Boolean = false) : EditorIntent
    data object ClearLasso : EditorIntent
    data class RemoveColorBackground(val targetColor: Int, val tolerance: Float = 0.25f) : EditorIntent

    data class OpenTextEditor(val initialProperties: TextProperties = TextProperties()) : EditorIntent
    data object CloseTextEditor : EditorIntent

    data object OpenExportDialog : EditorIntent
    data object CloseExportDialog : EditorIntent
    data object OpenResizeDialog : EditorIntent
    data object CloseResizeDialog : EditorIntent

    data object Undo : EditorIntent
    data object Redo : EditorIntent

    data class Export(
        val format: ExportEngine.ExportFormat,
        val scale: ExportEngine.ResolutionScale,
        val quality: Int
    ) : EditorIntent

    data object SaveProject : EditorIntent
    data object DismissMessage : EditorIntent
}
