package com.chromacraft.studio.core.graphics

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import androidx.compose.ui.geometry.Offset
import com.chromacraft.studio.core.model.LayerMask
import kotlin.math.hypot

/**
 * Motor de extracción de objetos, selección por lazo y eliminación inteligente de fondos.
 */
object CutoutEngine {

    /**
     * Rasteriza una selección poligonal trazada a mano alzada en una máscara alfa monocroma.
     */
    fun createLassoMask(
        width: Int,
        height: Int,
        points: List<Offset>,
        isInverted: Boolean = false
    ): Bitmap {
        val mask = Bitmap.createBitmap(width, height, Bitmap.Config.ALPHA_8)
        val canvas = Canvas(mask)

        if (isInverted) {
            canvas.drawColor(Color.WHITE)
        } else {
            canvas.drawColor(Color.TRANSPARENT)
        }

        if (points.size >= 3) {
            val path = Path().apply {
                moveTo(points[0].x, points[0].y)
                for (i in 1 until points.size) {
                    lineTo(points[i].x, points[i].y)
                }
                close()
            }

            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                style = Paint.Style.FILL
                if (isInverted) {
                    xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
                }
            }
            canvas.drawPath(path, paint)
        }

        return mask
    }

    /**
     * Eliminación de fondo basada en distancia de color cromático euclidiano (Chroma Key / Color Threshold).
     */
    fun removeColorBackground(
        srcBitmap: Bitmap,
        targetColor: Int,
        tolerance: Float = 0.25f // 0f a 1f
    ): Bitmap {
        val width = srcBitmap.width
        val height = srcBitmap.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val pixels = IntArray(width * height)
        srcBitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val tr = (targetColor ushr 16) and 0xFF
        val tg = (targetColor ushr 8) and 0xFF
        val tb = targetColor and 0xFF

        val maxDist = 441.67f // sqrt(255^2 + 255^2 + 255^2)
        val threshold = tolerance * maxDist

        for (i in pixels.indices) {
            val c = pixels[i]
            val a = (c ushr 24) and 0xFF
            val r = (c ushr 16) and 0xFF
            val g = (c ushr 8) and 0xFF
            val b = c and 0xFF

            val dist = hypot(hypot((r - tr).toFloat(), (g - tg).toFloat()), (b - tb).toFloat())

            if (dist < threshold) {
                // Suavizar borde (feather)
                val alphaFactor = (dist / threshold).coerceIn(0f, 1f)
                val newAlpha = (a * alphaFactor).toInt()
                pixels[i] = (newAlpha shl 24) or (c and 0x00FFFFFF)
            }
        }

        output.setPixels(pixels, 0, width, 0, 0, width, height)
        return output
    }

    /**
     * Aplica la máscara sobre un bitmap fuente, recortando el área protegida.
     */
    fun applyMaskToBitmap(srcBitmap: Bitmap, mask: LayerMask): Bitmap {
        if (!mask.isEnabled) return srcBitmap
        val width = srcBitmap.width
        val height = srcBitmap.height

        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        canvas.drawBitmap(srcBitmap, 0f, 0f, null)

        val maskBitmap = mask.maskBitmap ?: if (mask.lassoPoints.isNotEmpty()) {
            createLassoMask(width, height, mask.lassoPoints, mask.isInverted)
        } else null

        if (maskBitmap != null) {
            val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
            }
            canvas.drawBitmap(maskBitmap, 0f, 0f, maskPaint)
        }

        return output
    }
}
