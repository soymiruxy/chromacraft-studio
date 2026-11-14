package com.chromacraft.studio

import com.chromacraft.studio.core.graphics.ToneCurveEngine
import com.chromacraft.studio.core.model.CurvePoint
import com.chromacraft.studio.core.model.LevelsProperties
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ToneCurveEngineTest {

    @Test
    fun testDefaultLutIsLinearIdentity() {
        val points = listOf(CurvePoint(0f, 0f), CurvePoint(1f, 1f))
        val lut = ToneCurveEngine.buildChannelLut(points, LevelsProperties())
        assertEquals(256, lut.size)
        assertEquals(0, lut[0])
        assertEquals(127, lut[127], 2)
        assertEquals(255, lut[255])
    }

    @Test
    fun testLevelsBlackPointClamping() {
        val points = listOf(CurvePoint(0f, 0f), CurvePoint(1f, 1f))
        val levels = LevelsProperties(blackPoint = 50, gamma = 1.0f, whitePoint = 255)
        val lut = ToneCurveEngine.buildChannelLut(points, levels)
        assertEquals(0, lut[0])
        assertEquals(0, lut[50])
        // Valores superiores a 50 deben comenzar a subir gradualmente
        assertEquals(255, lut[255])
    }

    @Test
    fun testGammaAdjustmentCurve() {
        val points = listOf(CurvePoint(0f, 0f), CurvePoint(1f, 1f))
        // Gamma alto (> 1.0) aclara los tonos medios
        val levelsBright = LevelsProperties(blackPoint = 0, gamma = 2.0f, whitePoint = 255)
        val lutBright = ToneCurveEngine.buildChannelLut(points, levelsBright)

        val levelsNormal = LevelsProperties(blackPoint = 0, gamma = 1.0f, whitePoint = 255)
        val lutNormal = ToneCurveEngine.buildChannelLut(points, levelsNormal)

        assert(lutBright[128] > lutNormal[128])
    }
}
