package com.chromacraft.studio

import com.chromacraft.studio.core.graphics.FilterEngine
import com.chromacraft.studio.core.model.AdjustmentProperties
import com.chromacraft.studio.core.model.FilterPreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class FilterEngineTest {

    @Test
    fun testDefaultAdjustmentsProduceValidMatrix() {
        val adjustments = AdjustmentProperties()
        val matrix = FilterEngine.buildColorMatrix(adjustments, FilterPreset.NONE)
        assertNotNull(matrix)
        val array = matrix.array
        assertEquals(20, array.size)
        // La matriz identidad tiene 1f en la diagonal principal
        assertEquals(1f, array[0], 0.001f)
        assertEquals(1f, array[6], 0.001f)
        assertEquals(1f, array[12], 0.001f)
        assertEquals(1f, array[18], 0.001f)
    }

    @Test
    fun testBrightnessAdjustmentModifiesOffsetColumn() {
        val adjustments = AdjustmentProperties(brightness = 50f)
        val matrix = FilterEngine.buildColorMatrix(adjustments, FilterPreset.NONE)
        val array = matrix.array
        // Brillo 50 -> 50 * 2.55 = 127.5f en columna de traslación
        assertEquals(127.5f, array[4], 0.1f)
        assertEquals(127.5f, array[9], 0.1f)
        assertEquals(127.5f, array[14], 0.1f)
    }

    @Test
    fun testInvertPresetInvertsChannels() {
        val adjustments = AdjustmentProperties()
        val matrix = FilterEngine.buildColorMatrix(adjustments, FilterPreset.INVERT)
        val array = matrix.array
        assertEquals(-1f, array[0], 0.001f)
        assertEquals(255f, array[4], 0.001f)
    }
}
