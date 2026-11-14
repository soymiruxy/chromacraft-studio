package com.chromacraft.studio

import android.graphics.RectF
import com.chromacraft.studio.core.graphics.TransformEngine
import com.chromacraft.studio.core.model.CropRatio
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TransformEngineTest {

    @Test
    fun testComputeScaleAndRotation() {
        val (scale, deltaAngle) = TransformEngine.computeScaleAndRotation(
            centerX = 100f,
            centerY = 100f,
            touchX = 200f,
            touchY = 100f,
            initialDistance = 100f,
            initialAngleDegrees = 0f
        )
        // dx = 100, dy = 0 -> distance = 100 -> scale = 1.0f, angle = 0 deg
        assertEquals(1.0f, scale, 0.01f)
        assertEquals(0f, deltaAngle, 0.01f)
    }

    @Test
    fun testComputeConstrainedCropRectSquare() {
        val original = RectF(0f, 0f, 1920f, 1080f) // 16:9
        val squareCrop = TransformEngine.computeConstrainedCropRect(original, CropRatio.SQUARE_1_1)

        // El alto debe conservarse (1080) y el ancho recortarse a 1080 centrado
        assertEquals(1080f, squareCrop.height(), 0.01f)
        assertEquals(1080f, squareCrop.width(), 0.01f)
        val expectedOffset = (1920f - 1080f) / 2f
        assertEquals(expectedOffset, squareCrop.left, 0.01f)
    }
}
