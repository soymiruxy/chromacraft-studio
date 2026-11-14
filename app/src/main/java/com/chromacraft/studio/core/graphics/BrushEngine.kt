package com.chromacraft.studio.core.graphics

import android.graphics.BlurMaskFilter
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import androidx.compose.ui.geometry.Offset
import com.chromacraft.studio.core.model.BrushType
import com.chromacraft.studio.core.model.StrokePath

/**
 * Motor de trazado de pincel y pintura digital.
 * Soporta interpolación Bézier cuadrática, tipos de pincel (Lápiz, Aerógrafo, Caligrafía,
 * Rotulador, Spray) y sensibilidad a la presión del stylus / S-Pen.
 */
object BrushEngine {

    /**
     * Convierte una lista discreta de puntos en un Path continuo con curvas Bézier.
     */
    fun createSmoothPath(points: List<Offset>): Path {
        val path = Path()
        if (points.isEmpty()) return path

        if (points.size == 1) {
            val p = points[0]
            path.moveTo(p.x, p.y)
            path.lineTo(p.x + 0.1f, p.y + 0.1f)
            return path
        }

        path.moveTo(points[0].x, points[0].y)

        if (points.size == 2) {
            path.lineTo(points[1].x, points[1].y)
            return path
        }

        for (i in 1 until points.size) {
            val prev = points[i - 1]
            val curr = points[i]
            val midX = (prev.x + curr.x) / 2f
            val midY = (prev.y + curr.y) / 2f

            if (i == 1) {
                path.lineTo(midX, midY)
            } else {
                path.quadTo(prev.x, prev.y, midX, midY)
            }
        }

        val last = points.last()
        path.lineTo(last.x, last.y)
        return path
    }

    /**
     * Calcula el grosor promedio modulado por la presión del stylus si está disponible.
     */
    fun computePressureStrokeWidth(baseWidth: Float, pressures: List<Float>): Float {
        if (pressures.isEmpty()) return baseWidth
        val avgPressure = pressures.average().toFloat().coerceIn(0.1f, 1.0f)
        // Mapeo no lineal: presión ligera adelgaza a ~40%, presión fuerte engrosa a ~160%
        return baseWidth * (0.4f + 1.2f * avgPressure)
    }

    /**
     * Construye un objeto Paint configurado según la tipología de pincel elegida.
     */
    fun createPaintForStroke(stroke: StrokePath): Paint {
        val strokeW = computePressureStrokeWidth(stroke.strokeWidth, stroke.pressures)

        return Paint().apply {
            isAntiAlias = true
            isDither = true
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            strokeWidth = strokeW

            if (stroke.isEraser) {
                xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
                color = Color.TRANSPARENT
            } else {
                xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)
                val baseColor = stroke.color.toInt()
                val alpha = ((baseColor ushr 24) * stroke.opacity).toInt().coerceIn(0, 255)
                val rgb = baseColor and 0x00FFFFFF
                color = (alpha shl 24) or rgb

                // Personalización según el tipo de pincel
                when (stroke.brushType) {
                    BrushType.PENCIL -> {
                        strokeCap = Paint.Cap.ROUND
                    }
                    BrushType.AIRBRUSH -> {
                        // Borde difuso y plumoso
                        val blurRadius = (strokeW * 0.4f).coerceAtLeast(1f)
                        maskFilter = BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL)
                    }
                    BrushType.CALLIGRAPHY -> {
                        strokeCap = Paint.Cap.SQUARE
                        strokeWidth = strokeW * 1.2f
                    }
                    BrushType.MARKER -> {
                        // Tinta semitransparente acumulativa
                        val markerAlpha = (alpha * 0.7f).toInt().coerceIn(0, 255)
                        color = (markerAlpha shl 24) or rgb
                    }
                    BrushType.SPRAY -> {
                        val blur = (strokeW * 0.6f).coerceAtLeast(1f)
                        maskFilter = BlurMaskFilter(blur, BlurMaskFilter.Blur.SOLID)
                    }
                }
            }
        }
    }
}
