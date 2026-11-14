package com.chromacraft.studio.core.model

/**
 * Punto de control normalizado (0f a 1f) sobre la curva tonal.
 */
data class CurvePoint(
    val x: Float,
    val y: Float
) : Comparable<CurvePoint> {
    override fun compareTo(other: CurvePoint): Int = x.compareTo(other.x)
}

enum class CurveChannel(val displayName: String, val colorHex: Long) {
    RGB("RGB (Master)", 0xFFFFFFFF),
    RED("Rojo", 0xFFFF5252),
    GREEN("Verde", 0xFF00E676),
    BLUE("Azul", 0xFF448AFF)
}

/**
 * Niveles de entrada y salida para corrección de punto negro, gamma y punto blanco.
 */
data class LevelsProperties(
    val blackPoint: Int = 0,     // 0 a 255
    val gamma: Float = 1.0f,     // 0.2f a 3.0f (1.0f = lineal)
    val whitePoint: Int = 255    // 0 a 255
) {
    val isDefault: Boolean
        get() = blackPoint == 0 && gamma == 1.0f && whitePoint == 255
}

/**
 * Propiedades de curvas tonales paramétricas por canal.
 */
data class ToneCurveProperties(
    val rgbPoints: List<CurvePoint> = listOf(CurvePoint(0f, 0f), CurvePoint(1f, 1f)),
    val redPoints: List<CurvePoint> = listOf(CurvePoint(0f, 0f), CurvePoint(1f, 1f)),
    val greenPoints: List<CurvePoint> = listOf(CurvePoint(0f, 0f), CurvePoint(1f, 1f)),
    val bluePoints: List<CurvePoint> = listOf(CurvePoint(0f, 0f), CurvePoint(1f, 1f)),
    val levels: LevelsProperties = LevelsProperties()
) {
    val isDefault: Boolean
        get() = levels.isDefault &&
                rgbPoints.size == 2 && rgbPoints[0] == CurvePoint(0f, 0f) && rgbPoints[1] == CurvePoint(1f, 1f) &&
                redPoints.size == 2 &&
                greenPoints.size == 2 &&
                bluePoints.size == 2
}
