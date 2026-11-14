package com.chromacraft.studio.core.model

/**
 * Parámetros integrales de ajuste tonal, luminancia y filtrado fotográfico.
 * Todos los rangos continuos están normalizados entre -100f y +100f,
 * salvo el radio de desenfoque y nitidez (0f a 50f).
 */
data class AdjustmentProperties(
    val brightness: Float = 0f,    // -100f a +100f
    val contrast: Float = 0f,      // -100f a +100f
    val saturation: Float = 0f,    // -100f a +100f
    val temperature: Float = 0f,   // -100f (Frío) a +100f (Cálido)
    val tint: Float = 0f,          // -100f (Verde) a +100f (Magenta)
    val exposure: Float = 0f,      // -100f a +100f
    val shadows: Float = 0f,       // -100f a +100f (Recuperación/profundización de sombras)
    val highlights: Float = 0f,    // -100f a +100f (Control de altas luces)
    val vignette: Float = 0f,      // 0f a 100f
    val blurRadius: Float = 0f,    // 0f a 40f (Desenfoque gaussiano)
    val sharpen: Float = 0f        // 0f a 100f (Máscara de enfoque)
) {
    val isDefault: Boolean
        get() = brightness == 0f &&
                contrast == 0f &&
                saturation == 0f &&
                temperature == 0f &&
                tint == 0f &&
                exposure == 0f &&
                shadows == 0f &&
                highlights == 0f &&
                vignette == 0f &&
                blurRadius == 0f &&
                sharpen == 0f
}

/**
 * Preajustes estilísticos de filtros de imagen instantáneos (FX).
 */
enum class FilterPreset(val displayName: String, val category: String) {
    NONE("Original", "Básico"),
    VIVID("Vívido", "Color"),
    WARM("Cálido", "Color"),
    COOL("Frío", "Color"),
    DRAMA("Drama", "Tonal"),
    MONO("B&W", "Monocromo"),
    SEPIA("Sepia", "Vintage"),
    CYBERPUNK("Cyberpunk", "Artístico"),
    VINTAGE("Retro 90s", "Vintage"),
    DUOTONE("Duotono", "Artístico"),
    INVERT("Invertir", "Efectos")
}
