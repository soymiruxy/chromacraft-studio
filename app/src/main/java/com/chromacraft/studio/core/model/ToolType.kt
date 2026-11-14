package com.chromacraft.studio.core.model

/**
 * Categorías completas de herramientas profesionales del editor gráfico.
 */
enum class ToolType(val label: String, val iconCategory: String) {
    NONE("Ninguna", "system"),
    CROP("Recorte & Giro", "transform"),
    ADJUST("Ajustes", "color"),
    CURVES("Curvas & Niveles", "tonal"),
    FILTER("Filtros FX", "effects"),
    BLUR_SHARPEN("Desenfoque & Nitidez", "optics"),
    CUTOUT("Recorte & Fondo", "selection"),
    DRAW("Dibujo Digital", "artistic"),
    TEXT("Tipografía", "typography"),
    STICKER("Stickers & Formas", "elements"),
    LAYERS("Capas & Máscaras", "layers")
}
