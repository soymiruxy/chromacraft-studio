package com.chromacraft.studio.core.graphics

import android.graphics.Matrix
import android.graphics.RectF
import com.chromacraft.studio.core.model.CropRatio
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Motor de transformaciones afines bidimensionales, manejo de gestos táctiles,
 * volteo horizontal/vertical y cálculo trigonométrico del Gizmo de Picsart.
 */
object TransformEngine {

    enum class GizmoHandle {
        NONE,
        BODY,            // Arrastre traslacional del elemento
        DELETE,          // Esquina Superior Izquierda: Eliminar capa
        DUPLICATE,       // Esquina Superior Derecha: Duplicar capa
        SCALE_ROTATE,    // Esquina Inferior Derecha: Escalar y rotar con un solo dedo
        LOCK_TOGGLE      // Esquina Inferior Izquierda: Bloquear / desbloquear
    }

    /**
     * Construye una matriz de transformación afín centrada en el origen local del objeto,
     * incorporando traslación, rotación continua, escala y volteo especular (Flip H/V).
     */
    fun createLayerMatrix(
        intrinsicWidth: Float,
        intrinsicHeight: Float,
        translationX: Float,
        translationY: Float,
        scaleX: Float,
        scaleY: Float,
        rotationDegrees: Float,
        flipHorizontal: Boolean = false,
        flipVertical: Boolean = false
    ): Matrix {
        val matrix = Matrix()
        val halfW = intrinsicWidth / 2f
        val halfH = intrinsicHeight / 2f

        matrix.preTranslate(translationX, translationY)
        matrix.preRotate(rotationDegrees)

        val flipSx = if (flipHorizontal) -scaleX else scaleX
        val flipSy = if (flipVertical) -scaleY else scaleY
        matrix.preScale(flipSx, flipSy)

        matrix.preTranslate(-halfW, -halfH)

        return matrix
    }

    /**
     * Calcula los 4 puntos transformados de las esquinas del rectángulo delimitador.
     */
    fun getTransformedCorners(
        width: Float,
        height: Float,
        translationX: Float,
        translationY: Float,
        scaleX: Float,
        scaleY: Float,
        rotationDegrees: Float
    ): FloatArray {
        val halfW = (width / 2f) * scaleX
        val halfH = (height / 2f) * scaleY

        val rad = Math.toRadians(rotationDegrees.toDouble())
        val cosA = cos(rad).toFloat()
        val sinA = sin(rad).toFloat()

        val corners = arrayOf(
            Pair(-halfW, -halfH), // Top-Left
            Pair(halfW, -halfH),  // Top-Right
            Pair(halfW, halfH),   // Bottom-Right
            Pair(-halfW, halfH)   // Bottom-Left
        )

        val result = FloatArray(8)
        for (i in corners.indices) {
            val (rx, ry) = corners[i]
            val rotatedX = rx * cosA - ry * sinA
            val rotatedY = rx * sinA + ry * cosA
            result[i * 2] = translationX + rotatedX
            result[i * 2 + 1] = translationY + rotatedY
        }
        return result
    }

    /**
     * Detección de colisión táctil (Hit Testing) sobre los manejadores del Gizmo interactivo.
     */
    fun hitTestGizmo(
        touchX: Float,
        touchY: Float,
        corners: FloatArray,
        handleRadiusPx: Float = 48f
    ): GizmoHandle {
        val tlX = corners[0]; val tlY = corners[1]
        val trX = corners[2]; val trY = corners[3]
        val brX = corners[4]; val brY = corners[5]
        val blX = corners[6]; val blY = corners[7]

        if (hypot(touchX - tlX, touchY - tlY) <= handleRadiusPx) return GizmoHandle.DELETE
        if (hypot(touchX - trX, touchY - trY) <= handleRadiusPx) return GizmoHandle.DUPLICATE
        if (hypot(touchX - brX, touchY - brY) <= handleRadiusPx) return GizmoHandle.SCALE_ROTATE
        if (hypot(touchX - blX, touchY - blY) <= handleRadiusPx) return GizmoHandle.LOCK_TOGGLE

        if (isPointInPolygon(touchX, touchY, corners)) {
            return GizmoHandle.BODY
        }

        return GizmoHandle.NONE
    }

    private fun isPointInPolygon(px: Float, py: Float, corners: FloatArray): Boolean {
        var inside = false
        var j = 3
        for (i in 0..3) {
            val ix = corners[i * 2]
            val iy = corners[i * 2 + 1]
            val jx = corners[j * 2]
            val jy = corners[j * 2 + 1]

            if ((iy > py) != (jy > py) &&
                px < (jx - ix) * (py - iy) / (jy - iy) + ix
            ) {
                inside = !inside
            }
            j = i
        }
        return inside
    }

    fun computeScaleAndRotation(
        centerX: Float,
        centerY: Float,
        touchX: Float,
        touchY: Float,
        initialDistance: Float,
        initialAngleDegrees: Float
    ): Pair<Float, Float> {
        val dx = touchX - centerX
        val dy = touchY - centerY

        val currentDist = hypot(dx, dy)
        val currentAngleRad = atan2(dy, dx)
        val currentAngleDeg = Math.toDegrees(currentAngleRad.toDouble()).toFloat()

        val newScale = if (initialDistance > 0.001f) {
            (currentDist / initialDistance).coerceIn(0.15f, 10f)
        } else 1.0f

        val newRotation = currentAngleDeg - initialAngleDegrees

        return Pair(newScale, newRotation)
    }

    fun computeConstrainedCropRect(
        bounds: RectF,
        cropRatio: CropRatio
    ): RectF {
        val ratio = cropRatio.ratio ?: return RectF(bounds)
        val boundsRatio = bounds.width() / bounds.height()

        return if (boundsRatio > ratio) {
            val targetWidth = bounds.height() * ratio
            val offsetX = (bounds.width() - targetWidth) / 2f
            RectF(bounds.left + offsetX, bounds.top, bounds.right - offsetX, bounds.bottom)
        } else {
            val targetHeight = bounds.width() / ratio
            val offsetY = (bounds.height() - targetHeight) / 2f
            RectF(bounds.left, bounds.top + offsetY, bounds.right, bounds.bottom - offsetY)
        }
    }
}
