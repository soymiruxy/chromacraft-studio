package com.chromacraft.studio

import android.graphics.BitmapFactory
import com.chromacraft.studio.core.graphics.BitmapUtils
import org.junit.Assert.assertEquals
import org.junit.Test

class BitmapUtilsTest {

    @Test
    fun testCalculateInSampleSizeWhenSmallerThanRequested() {
        val options = BitmapFactory.Options().apply {
            outWidth = 800
            outHeight = 600
        }
        val sampleSize = BitmapUtils.calculateInSampleSize(options, 1024, 1024)
        assertEquals(1, sampleSize)
    }

    @Test
    fun testCalculateInSampleSizeForHighResCameraPhoto() {
        val options = BitmapFactory.Options().apply {
            outWidth = 4000
            outHeight = 3000
        }
        // Para caber dentro de 2048x2048 debe submuestrear a 2
        val sampleSize = BitmapUtils.calculateInSampleSize(options, 2048, 2048)
        assertEquals(2, sampleSize)
    }

    @Test
    fun testCalculateInSampleSizeFor8kPhoto() {
        val options = BitmapFactory.Options().apply {
            outWidth = 8000
            outHeight = 6000
        }
        // Debe ser potencia de 2: inSampleSize = 4 (8000/4 = 2000 <= 2048)
        val sampleSize = BitmapUtils.calculateInSampleSize(options, 2048, 2048)
        assertEquals(4, sampleSize)
    }
}
