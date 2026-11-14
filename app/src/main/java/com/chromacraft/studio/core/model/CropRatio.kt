package com.chromacraft.studio.core.model

/**
 * Preajustes de relaciones de aspecto estándar para recortes en redes sociales y fotografía.
 */
enum class CropRatio(val label: String, val ratio: Float?) {
    FREE("Libre", null),
    SQUARE_1_1("1:1 Cuadrado", 1.0f),
    PORTRAIT_4_5("4:5 Post", 4f / 5f),
    STORY_9_16("9:16 Historia", 9f / 16f),
    LANDSCAPE_16_9("16:9 Portada", 16f / 9f),
    PHOTO_3_4("3:4 Estándar", 3f / 4f)
}
