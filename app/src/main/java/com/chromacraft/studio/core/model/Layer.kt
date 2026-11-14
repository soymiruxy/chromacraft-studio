package com.chromacraft.studio.core.model

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import java.util.UUID

/**
 * Máscara de capa no destructiva basada en canal alfa o trazado vectorial.
 */
data class LayerMask(
    val id: String = UUID.randomUUID().toString(),
    val maskBitmap: Bitmap? = null,
    val lassoPoints: List<Offset> = emptyList(),
    val isInverted: Boolean = false,
    val isEnabled: Boolean = true
)

/**
 * Abstracción inmutable para cualquier elemento en la pila de capas del editor gráfico.
 */
sealed class Layer(
    open val id: String = UUID.randomUUID().toString(),
    open val name: String,
    open val isVisible: Boolean = true,
    open val isLocked: Boolean = false,
    open val opacity: Float = 1.0f,
    open val blendMode: BlendModeType = BlendModeType.NORMAL,
    open val translationX: Float = 0f,
    open val translationY: Float = 0f,
    open val scaleX: Float = 1.0f,
    open val scaleY: Float = 1.0f,
    open val rotationDegrees: Float = 0f,
    open val flipHorizontal: Boolean = false,
    open val flipVertical: Boolean = false,
    open val mask: LayerMask? = null
) {
    abstract fun copyWithTransform(
        translationX: Float = this.translationX,
        translationY: Float = this.translationY,
        scaleX: Float = this.scaleX,
        scaleY: Float = this.scaleY,
        rotationDegrees: Float = this.rotationDegrees,
        flipHorizontal: Boolean = this.flipHorizontal,
        flipVertical: Boolean = this.flipVertical
    ): Layer

    abstract fun copyWithVisibility(isVisible: Boolean): Layer
    abstract fun copyWithOpacity(opacity: Float): Layer
    abstract fun copyWithBlendMode(blendMode: BlendModeType): Layer
    abstract fun copyWithMask(mask: LayerMask?): Layer
    abstract fun copyWithLock(isLocked: Boolean): Layer
}

/**
 * Capa de mapa de bits (fotografía de fondo o imagen importada).
 */
data class ImageLayer(
    override val id: String = UUID.randomUUID().toString(),
    override val name: String = "Imagen",
    override val isVisible: Boolean = true,
    override val isLocked: Boolean = false,
    override val opacity: Float = 1.0f,
    override val blendMode: BlendModeType = BlendModeType.NORMAL,
    override val translationX: Float = 0f,
    override val translationY: Float = 0f,
    override val scaleX: Float = 1.0f,
    override val scaleY: Float = 1.0f,
    override val rotationDegrees: Float = 0f,
    override val flipHorizontal: Boolean = false,
    override val flipVertical: Boolean = false,
    override val mask: LayerMask? = null,
    val bitmap: Bitmap? = null,
    val intrinsicWidth: Int = 1080,
    val intrinsicHeight: Int = 1080,
    val adjustments: AdjustmentProperties = AdjustmentProperties(),
    val curves: ToneCurveProperties = ToneCurveProperties(),
    val filterPreset: FilterPreset = FilterPreset.NONE
) : Layer(
    id, name, isVisible, isLocked, opacity, blendMode,
    translationX, translationY, scaleX, scaleY, rotationDegrees,
    flipHorizontal, flipVertical, mask
) {
    override fun copyWithTransform(
        translationX: Float,
        translationY: Float,
        scaleX: Float,
        scaleY: Float,
        rotationDegrees: Float,
        flipHorizontal: Boolean,
        flipVertical: Boolean
    ): ImageLayer = copy(
        translationX = translationX,
        translationY = translationY,
        scaleX = scaleX,
        scaleY = scaleY,
        rotationDegrees = rotationDegrees,
        flipHorizontal = flipHorizontal,
        flipVertical = flipVertical
    )

    override fun copyWithVisibility(isVisible: Boolean): ImageLayer = copy(isVisible = isVisible)
    override fun copyWithOpacity(opacity: Float): ImageLayer = copy(opacity = opacity)
    override fun copyWithBlendMode(blendMode: BlendModeType): ImageLayer = copy(blendMode = blendMode)
    override fun copyWithMask(mask: LayerMask?): ImageLayer = copy(mask = mask)
    override fun copyWithLock(isLocked: Boolean): ImageLayer = copy(isLocked = isLocked)
}

/**
 * Capa de trazos de pintura y dibujo digital.
 */
data class DrawingLayer(
    override val id: String = UUID.randomUUID().toString(),
    override val name: String = "Dibujo",
    override val isVisible: Boolean = true,
    override val isLocked: Boolean = false,
    override val opacity: Float = 1.0f,
    override val blendMode: BlendModeType = BlendModeType.NORMAL,
    override val translationX: Float = 0f,
    override val translationY: Float = 0f,
    override val scaleX: Float = 1.0f,
    override val scaleY: Float = 1.0f,
    override val rotationDegrees: Float = 0f,
    override val flipHorizontal: Boolean = false,
    override val flipVertical: Boolean = false,
    override val mask: LayerMask? = null,
    val strokes: List<StrokePath> = emptyList()
) : Layer(
    id, name, isVisible, isLocked, opacity, blendMode,
    translationX, translationY, scaleX, scaleY, rotationDegrees,
    flipHorizontal, flipVertical, mask
) {
    override fun copyWithTransform(
        translationX: Float,
        translationY: Float,
        scaleX: Float,
        scaleY: Float,
        rotationDegrees: Float,
        flipHorizontal: Boolean,
        flipVertical: Boolean
    ): DrawingLayer = copy(
        translationX = translationX,
        translationY = translationY,
        scaleX = scaleX,
        scaleY = scaleY,
        rotationDegrees = rotationDegrees,
        flipHorizontal = flipHorizontal,
        flipVertical = flipVertical
    )

    override fun copyWithVisibility(isVisible: Boolean): DrawingLayer = copy(isVisible = isVisible)
    override fun copyWithOpacity(opacity: Float): DrawingLayer = copy(opacity = opacity)
    override fun copyWithBlendMode(blendMode: BlendModeType): DrawingLayer = copy(blendMode = blendMode)
    override fun copyWithMask(mask: LayerMask?): DrawingLayer = copy(mask = mask)
    override fun copyWithLock(isLocked: Boolean): DrawingLayer = copy(isLocked = isLocked)
}

/**
 * Capa de tipografía vectorial con formato rico editable.
 */
data class TextLayer(
    override val id: String = UUID.randomUUID().toString(),
    override val name: String = "Texto",
    override val isVisible: Boolean = true,
    override val isLocked: Boolean = false,
    override val opacity: Float = 1.0f,
    override val blendMode: BlendModeType = BlendModeType.NORMAL,
    override val translationX: Float = 0f,
    override val translationY: Float = 0f,
    override val scaleX: Float = 1.0f,
    override val scaleY: Float = 1.0f,
    override val rotationDegrees: Float = 0f,
    override val flipHorizontal: Boolean = false,
    override val flipVertical: Boolean = false,
    override val mask: LayerMask? = null,
    val textProperties: TextProperties = TextProperties()
) : Layer(
    id, name, isVisible, isLocked, opacity, blendMode,
    translationX, translationY, scaleX, scaleY, rotationDegrees,
    flipHorizontal, flipVertical, mask
) {
    override fun copyWithTransform(
        translationX: Float,
        translationY: Float,
        scaleX: Float,
        scaleY: Float,
        rotationDegrees: Float,
        flipHorizontal: Boolean,
        flipVertical: Boolean
    ): TextLayer = copy(
        translationX = translationX,
        translationY = translationY,
        scaleX = scaleX,
        scaleY = scaleY,
        rotationDegrees = rotationDegrees,
        flipHorizontal = flipHorizontal,
        flipVertical = flipVertical
    )

    override fun copyWithVisibility(isVisible: Boolean): TextLayer = copy(isVisible = isVisible)
    override fun copyWithOpacity(opacity: Float): TextLayer = copy(opacity = opacity)
    override fun copyWithBlendMode(blendMode: BlendModeType): TextLayer = copy(blendMode = blendMode)
    override fun copyWithMask(mask: LayerMask?): TextLayer = copy(mask = mask)
    override fun copyWithLock(isLocked: Boolean): TextLayer = copy(isLocked = isLocked)
}

/**
 * Capa de figuras vectoriales, stickers y distintivos gráficos.
 */
data class StickerLayer(
    override val id: String = UUID.randomUUID().toString(),
    override val name: String = "Sticker",
    override val isVisible: Boolean = true,
    override val isLocked: Boolean = false,
    override val opacity: Float = 1.0f,
    override val blendMode: BlendModeType = BlendModeType.NORMAL,
    override val translationX: Float = 0f,
    override val translationY: Float = 0f,
    override val scaleX: Float = 1.0f,
    override val scaleY: Float = 1.0f,
    override val rotationDegrees: Float = 0f,
    override val flipHorizontal: Boolean = false,
    override val flipVertical: Boolean = false,
    override val mask: LayerMask? = null,
    val stickerProperties: StickerProperties = StickerProperties()
) : Layer(
    id, name, isVisible, isLocked, opacity, blendMode,
    translationX, translationY, scaleX, scaleY, rotationDegrees,
    flipHorizontal, flipVertical, mask
) {
    override fun copyWithTransform(
        translationX: Float,
        translationY: Float,
        scaleX: Float,
        scaleY: Float,
        rotationDegrees: Float,
        flipHorizontal: Boolean,
        flipVertical: Boolean
    ): StickerLayer = copy(
        translationX = translationX,
        translationY = translationY,
        scaleX = scaleX,
        scaleY = scaleY,
        rotationDegrees = rotationDegrees,
        flipHorizontal = flipHorizontal,
        flipVertical = flipVertical
    )

    override fun copyWithVisibility(isVisible: Boolean): StickerLayer = copy(isVisible = isVisible)
    override fun copyWithOpacity(opacity: Float): StickerLayer = copy(opacity = opacity)
    override fun copyWithBlendMode(blendMode: BlendModeType): StickerLayer = copy(blendMode = blendMode)
    override fun copyWithMask(mask: LayerMask?): StickerLayer = copy(mask = mask)
    override fun copyWithLock(isLocked: Boolean): StickerLayer = copy(isLocked = isLocked)
}
