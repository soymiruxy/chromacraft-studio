package com.chromacraft.studio.core.model

import androidx.compose.ui.geometry.Offset
import java.util.UUID

/**
 * Tipologías de pincel con dinámicas de dispersión y dureza características.
 */
enum class BrushType(val displayName: String) {
    PENCIL("Lápiz Fino"),
    AIRBRUSH("Aerógrafo"),
    CALLIGRAPHY("Caligrafía"),
    MARKER("Rotulador"),
    SPRAY("Spray")
}

/**
 * Propiedades del pincel en el motor de pintura digital.
 */
data class BrushProperties(
    val brushType: BrushType = BrushType.PENCIL,
    val size: Float = 18f,
    val opacity: Float = 1.0f,
    val color: Long = 0xFFFFFFFF,
    val isEraser: Boolean = false,
    val pressureSensitivity: Boolean = true,
    val smoothness: Boolean = true
)

/**
 * Representación vectorial de un trazo de pincel con soporte de presión de stylus.
 */
data class StrokePath(
    val id: String = UUID.randomUUID().toString(),
    val points: List<Offset> = emptyList(),
    val pressures: List<Float> = emptyList(),
    val brushType: BrushType = BrushType.PENCIL,
    val color: Long = 0xFFFFFFFF,
    val strokeWidth: Float = 18f,
    val opacity: Float = 1.0f,
    val isEraser: Boolean = false
)

/**
 * Formas geométricas y stickers vectoriales.
 */
enum class StickerShapeType(val label: String) {
    RECTANGLE("Rectángulo"),
    ROUNDED_RECT("Rect. Redondeado"),
    CIRCLE("Círculo"),
    STAR("Estrella"),
    HEART("Corazón"),
    POLYGON("Hexágono"),
    BADGE("Medalla"),
    SPEECH_BUBBLE("Bocadillo"),
    ARROW("Flecha"),
    FIRE("Fuego"),
    LIGHTNING("Rayo")
}

data class StickerProperties(
    val shapeType: StickerShapeType = StickerShapeType.STAR,
    val fillColor: Long = 0xFFFFD700,
    val strokeColor: Long = 0xFFFFFFFF,
    val strokeWidth: Float = 4f
)

data class TextProperties(
    val text: String = "Texto Creativo",
    val fontSize: Float = 46f,
    val textColor: Long = 0xFFFFFFFF,
    val strokeColor: Long? = 0xFF000000,
    val strokeWidth: Float = 0f,
    val shadowRadius: Float = 6f,
    val shadowColor: Long? = 0x88000000,
    val backgroundColor: Long? = null,
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val letterSpacing: Float = 0f
)
