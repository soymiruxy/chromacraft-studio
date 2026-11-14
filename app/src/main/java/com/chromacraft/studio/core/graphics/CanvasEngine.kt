package com.chromacraft.studio.core.graphics

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.Typeface
import com.chromacraft.studio.core.model.DrawingLayer
import com.chromacraft.studio.core.model.ImageLayer
import com.chromacraft.studio.core.model.Layer
import com.chromacraft.studio.core.model.StickerLayer
import com.chromacraft.studio.core.model.StickerShapeType
import com.chromacraft.studio.core.model.TextLayer
import kotlin.math.cos
import kotlin.math.sin

/**
 * Motor central de composición gráfica y renderizado de capas.
 * Ejecuta la síntesis de capas respetando transformaciones afines, volteos H/V,
 * máscaras de capa no destructivas, modos de fusión, curvas tonales y primitivas vectoriales.
 */
object CanvasEngine {

    /**
     * Renderiza la lista completa de capas visibles sobre el lienzo proporcionado.
     */
    fun renderLayers(
        canvas: Canvas,
        layers: List<Layer>,
        targetWidth: Int,
        targetHeight: Int,
        drawBackground: Boolean = true
    ) {
        if (drawBackground) {
            canvas.drawColor(0xFF0D0E12.toInt())
        } else {
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        }

        for (layer in layers) {
            if (!layer.isVisible || layer.opacity <= 0.01f) continue

            val saveCount = canvas.save()

            // 1. Aplicar la matriz afín de la capa con Flip H/V
            val matrix = TransformEngine.createLayerMatrix(
                intrinsicWidth = targetWidth.toFloat(),
                intrinsicHeight = targetHeight.toFloat(),
                translationX = layer.translationX,
                translationY = layer.translationY,
                scaleX = layer.scaleX,
                scaleY = layer.scaleY,
                rotationDegrees = layer.rotationDegrees,
                flipHorizontal = layer.flipHorizontal,
                flipVertical = layer.flipVertical
            )
            canvas.concat(matrix)

            // 2. Renderizar según el tipo concreto de capa
            when (layer) {
                is ImageLayer -> renderImageLayer(canvas, layer, targetWidth, targetHeight)
                is DrawingLayer -> renderDrawingLayer(canvas, layer)
                is TextLayer -> renderTextLayer(canvas, layer, targetWidth, targetHeight)
                is StickerLayer -> renderStickerLayer(canvas, layer, targetWidth, targetHeight)
            }

            canvas.restoreToCount(saveCount)
        }
    }

    private fun renderImageLayer(
        canvas: Canvas,
        layer: ImageLayer,
        width: Int,
        height: Int
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            alpha = (layer.opacity * 255).toInt().coerceIn(0, 255)
            xfermode = FilterEngine.toXfermode(layer.blendMode)
            colorFilter = FilterEngine.createColorFilter(layer.adjustments, layer.filterPreset)
        }

        var bmp = layer.bitmap
        if (bmp != null && !bmp.isRecycled) {
            // Aplicar curvas tonales si están configuradas
            if (!layer.curves.isDefault) {
                bmp = ToneCurveEngine.applyCurves(bmp, layer.curves)
            }

            // Aplicar máscara si está activa
            val mask = layer.mask
            if (mask != null && mask.isEnabled) {
                bmp = CutoutEngine.applyMaskToBitmap(bmp, mask)
            }

            val destRect = RectF(0f, 0f, width.toFloat(), height.toFloat())
            canvas.drawBitmap(bmp, null, destRect, paint)
        } else {
            val demoPaint = Paint().apply {
                color = Color.DKGRAY
                style = Paint.Style.FILL
            }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), demoPaint)
        }
    }

    private fun renderDrawingLayer(
        canvas: Canvas,
        layer: DrawingLayer
    ) {
        for (stroke in layer.strokes) {
            val paint = BrushEngine.createPaintForStroke(stroke)
            val path = BrushEngine.createSmoothPath(stroke.points)
            canvas.drawPath(path, paint)
        }
    }

    private fun renderTextLayer(
        canvas: Canvas,
        layer: TextLayer,
        canvasWidth: Int,
        canvasHeight: Int
    ) {
        val props = layer.textProperties
        val centerX = canvasWidth / 2f
        val centerY = canvasHeight / 2f

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = props.textColor.toInt()
            textSize = props.fontSize
            textAlign = Paint.Align.CENTER
            typeface = if (props.isBold && props.isItalic) {
                Typeface.create(Typeface.DEFAULT, Typeface.BOLD_ITALIC)
            } else if (props.isBold) {
                Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            } else if (props.isItalic) {
                Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            } else {
                Typeface.DEFAULT
            }
            alpha = (layer.opacity * 255).toInt().coerceIn(0, 255)
            xfermode = FilterEngine.toXfermode(layer.blendMode)

            if (props.shadowColor != null && props.shadowRadius > 0f) {
                setShadowLayer(props.shadowRadius, 2f, 3f, props.shadowColor.toInt())
            }
        }

        // Borde opcional
        if (props.strokeColor != null && props.strokeWidth > 0f) {
            val strokePaint = Paint(textPaint).apply {
                style = Paint.Style.STROKE
                strokeWidth = props.strokeWidth
                color = props.strokeColor.toInt()
                clearShadowLayer()
            }
            canvas.drawText(props.text, centerX, centerY, strokePaint)
        }

        // Fondo de pastilla opcional
        if (props.backgroundColor != null) {
            val textWidth = textPaint.measureText(props.text)
            val fm = textPaint.fontMetrics
            val padX = 24f
            val padY = 16f
            val bgRect = RectF(
                centerX - textWidth / 2f - padX,
                centerY + fm.ascent - padY,
                centerX + textWidth / 2f + padX,
                centerY + fm.descent + padY
            )
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = props.backgroundColor.toInt()
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(bgRect, 16f, 16f, bgPaint)
        }

        canvas.drawText(props.text, centerX, centerY, textPaint)
    }

    private fun renderStickerLayer(
        canvas: Canvas,
        layer: StickerLayer,
        canvasWidth: Int,
        canvasHeight: Int
    ) {
        val props = layer.stickerProperties
        val centerX = canvasWidth / 2f
        val centerY = canvasHeight / 2f
        val size = 120f

        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = props.fillColor.toInt()
            style = Paint.Style.FILL
            alpha = (layer.opacity * 255).toInt().coerceIn(0, 255)
            xfermode = FilterEngine.toXfermode(layer.blendMode)
        }

        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = props.strokeColor.toInt()
            style = Paint.Style.STROKE
            strokeWidth = props.strokeWidth
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        val shapePath = createStickerPath(props.shapeType, centerX, centerY, size)
        canvas.drawPath(shapePath, fillPaint)
        if (props.strokeWidth > 0f) {
            canvas.drawPath(shapePath, strokePaint)
        }
    }

    private fun createStickerPath(
        type: StickerShapeType,
        cx: Float,
        cy: Float,
        radius: Float
    ): Path {
        val path = Path()
        when (type) {
            StickerShapeType.RECTANGLE -> {
                path.addRect(cx - radius, cy - radius * 0.75f, cx + radius, cy + radius * 0.75f, Path.Direction.CW)
            }

            StickerShapeType.ROUNDED_RECT -> {
                val rect = RectF(cx - radius, cy - radius * 0.75f, cx + radius, cy + radius * 0.75f)
                path.addRoundRect(rect, 24f, 24f, Path.Direction.CW)
            }

            StickerShapeType.CIRCLE -> {
                path.addCircle(cx, cy, radius, Path.Direction.CW)
            }

            StickerShapeType.STAR -> {
                val points = 5
                val innerRadius = radius * 0.45f
                val step = Math.PI / points
                var angle = -Math.PI / 2
                path.moveTo((cx + radius * cos(angle)).toFloat(), (cy + radius * sin(angle)).toFloat())
                for (i in 0 until points * 2) {
                    val r = if (i % 2 == 0) innerRadius else radius
                    angle += step
                    path.lineTo((cx + r * cos(angle)).toFloat(), (cy + r * sin(angle)).toFloat())
                }
                path.close()
            }

            StickerShapeType.HEART -> {
                val w = radius * 1.6f
                val h = radius * 1.6f
                path.moveTo(cx, cy + h * 0.35f)
                path.cubicTo(cx - w * 0.7f, cy - h * 0.3f, cx - w * 0.5f, cy - h * 0.8f, cx, cy - h * 0.4f)
                path.cubicTo(cx + w * 0.5f, cy - h * 0.8f, cx + w * 0.7f, cy - h * 0.3f, cx, cy + h * 0.35f)
                path.close()
            }

            StickerShapeType.POLYGON -> {
                val sides = 6
                val step = Math.PI * 2 / sides
                path.moveTo(cx + radius, cy)
                for (i in 1 until sides) {
                    val a = i * step
                    path.lineTo((cx + radius * cos(a)).toFloat(), (cy + radius * sin(a)).toFloat())
                }
                path.close()
            }

            StickerShapeType.BADGE -> {
                val numPoints = 12
                val innerRadius = radius * 0.85f
                val step = Math.PI / numPoints
                var angle = 0.0
                path.moveTo((cx + radius * cos(angle)).toFloat(), (cy + radius * sin(angle)).toFloat())
                for (i in 0 until numPoints * 2) {
                    val r = if (i % 2 == 0) innerRadius else radius
                    angle += step
                    path.lineTo((cx + r * cos(angle)).toFloat(), (cy + r * sin(angle)).toFloat())
                }
                path.close()
            }

            StickerShapeType.SPEECH_BUBBLE -> {
                val rect = RectF(cx - radius * 1.2f, cy - radius * 0.8f, cx + radius * 1.2f, cy + radius * 0.6f)
                path.addRoundRect(rect, 24f, 24f, Path.Direction.CW)
                path.moveTo(cx - radius * 0.4f, cy + radius * 0.6f)
                path.lineTo(cx - radius * 0.6f, cy + radius * 1.1f)
                path.lineTo(cx - radius * 0.1f, cy + radius * 0.6f)
            }

            StickerShapeType.ARROW -> {
                val w = radius * 1.5f
                val h = radius * 0.7f
                path.moveTo(cx - w * 0.5f, cy - h * 0.3f)
                path.lineTo(cx + w * 0.1f, cy - h * 0.3f)
                path.lineTo(cx + w * 0.1f, cy - h * 0.7f)
                path.lineTo(cx + w * 0.6f, cy)
                path.lineTo(cx + w * 0.1f, cy + h * 0.7f)
                path.lineTo(cx + w * 0.1f, cy + h * 0.3f)
                path.lineTo(cx - w * 0.5f, cy + h * 0.3f)
                path.close()
            }

            StickerShapeType.FIRE -> {
                path.moveTo(cx, cy + radius)
                path.cubicTo(cx - radius * 0.8f, cy + radius * 0.4f, cx - radius * 0.6f, cy - radius * 0.4f, cx - radius * 0.1f, cy - radius * 0.8f)
                path.cubicTo(cx - radius * 0.2f, cy - radius * 0.2f, cx + radius * 0.1f, cy - radius * 0.1f, cx + radius * 0.2f, cy - radius * 0.6f)
                path.cubicTo(cx + radius * 0.8f, cy - radius * 0.2f, cx + radius * 0.7f, cy + radius * 0.5f, cx, cy + radius)
                path.close()
            }

            StickerShapeType.LIGHTNING -> {
                path.moveTo(cx + radius * 0.1f, cy - radius)
                path.lineTo(cx - radius * 0.6f, cy + radius * 0.1f)
                path.lineTo(cx - radius * 0.1f, cy + radius * 0.1f)
                path.lineTo(cx - radius * 0.3f, cy + radius)
                path.lineTo(cx + radius * 0.6f, cy - radius * 0.2f)
                path.lineTo(cx + radius * 0.1f, cy - radius * 0.2f)
                path.close()
            }
        }
        return path
    }

    /**
     * Renderizado offscreen a resolución completa con control de transparencia.
     */
    fun renderFullResolution(
        layers: List<Layer>,
        width: Int,
        height: Int,
        keepTransparentBackground: Boolean = false
    ): Bitmap {
        val outputBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(outputBitmap)
        renderLayers(canvas, layers, width, height, drawBackground = !keepTransparentBackground)
        return outputBitmap
    }
}
