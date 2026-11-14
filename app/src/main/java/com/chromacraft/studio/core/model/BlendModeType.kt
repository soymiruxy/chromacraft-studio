package com.chromacraft.studio.core.model

/**
 * Modos de fusión de capas profesionales.
 */
enum class BlendModeType(val displayName: String) {
    NORMAL("Normal"),
    MULTIPLY("Multiplicar"),
    SCREEN("Trama"),
    OVERLAY("Superponer"),
    SOFT_LIGHT("Luz Suave"),
    DARKEN("Oscurecer"),
    LIGHTEN("Aclarar"),
    COLOR_DODGE("Sobreexponer Color"),
    DIFFERENCE("Diferencia"),
    ADD("Añadir")
}
