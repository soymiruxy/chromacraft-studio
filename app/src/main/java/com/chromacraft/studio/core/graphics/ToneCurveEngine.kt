package com.chromacraft.studio.core.graphics

import android.graphics.Bitmap
import com.chromacraft.studio.core.model.CurvePoint
import com.chromacraft.studio.core.model.LevelsProperties
import com.chromacraft.studio.core.model.ToneCurveProperties
import kotlin.math.pow

/**
 * Motor de evaluación y mapeo de curvas tonales (Tone Curves) y niveles (Levels).
 * Genera tablas de consulta (LUT - Look-Up Tables) de 256 entradas para ejecución en GPU/CPU.
 */
object ToneCurveEngine {

    /**
     * Genera una tabla de consulta (LUT) de 256 valores para un canal dado.
     */
    fun buildChannelLut(
        points: List<CurvePoint>,
        levels: LevelsProperties = LevelsProperties()
    ): IntArray {
        val lut = IntArray(256)
        val sorted = points.sorted()

        // 1. Evaluación por interpolación lineal por tramos o spline
        for (i in 0..255) {
            val normalizedIn = i / 255f
            var outY = normalizedIn

            // Buscar el segmento correspondiente
            for (p in 0 until sorted.size - 1) {
                val p0 = sorted[p]
                val p1 = sorted[p + 1]
                if (normalizedIn in p0.x..p1.x) {
                    val rangeX = (p1.x - p0.x).coerceAtLeast(0.0001f)
                    val t = (normalizedIn - p0.x) / rangeX
                    // Interpolación suave cúbica (Hermite)
                    val smoothT = t * t * (3f - 2f * t)
                    outY = p0.y + smoothT * (p1.y - p0.y)
                    break
                }
            }

            // 2. Aplicar corrección de Niveles (BlackPoint, Gamma, WhitePoint)
            var pixelVal = (outY * 255f).coerceIn(0f, 255f)
            if (!levels.isDefault) {
                val bp = levels.blackPoint.toFloat()
                val wp = levels.whitePoint.toFloat().coerceAtLeast(bp + 1f)
                val clamped = pixelVal.coerceIn(bp, wp)
                val norm = (clamped - bp) / (wp - bp)
                val gammaCorrected = norm.pow(1.0f / levels.gamma.coerceAtLeast(0.1f))
                pixelVal = gammaCorrected * 255f
            }

            lut[i] = pixelVal.toInt().coerceIn(0, 255)
        }
        return lut
    }

    /**
     * Aplica la transformación completa de curvas RGB y niveles a un mapa de bits.
     */
    fun applyCurves(
        srcBitmap: Bitmap,
        properties: ToneCurveProperties
    ): Bitmap {
        if (properties.isDefault) return srcBitmap

        val width = srcBitmap.width
        val height = srcBitmap.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val masterLut = buildChannelLut(properties.rgbPoints, properties.levels)
        val redLut = buildChannelLut(properties.redPoints)
        val greenLut = buildChannelLut(properties.greenPoints)
        val blueLut = buildChannelLut(properties.bluePoints)

        val pixels = IntArray(width * height)
        srcBitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        for (i in pixels.indices) {
            val c = pixels[i]
            val a = (c ushr 24) and 0xFF
            var r = (c ushr 16) and 0xFF
            var g = (c ushr 8) and 0xFF
            var b = c and 0xFF

            // Mapeo individual de canal
            r = redLut[r]
            g = greenLut[g]
            b = blueLut[b]

            // Mapeo master RGB
            r = masterLut[r]
            g = masterLut[g]
            b = masterLut[b]

            pixels[i] = (a shl 24) or (r shl 16) or (g shl 8) or b
        }

        output.setPixels(pixels, 0, width, 0, 0, width, height)
        return output
    }
}
