package com.chromacraft.studio

import com.chromacraft.studio.core.graphics.HistoryEngine
import com.chromacraft.studio.core.model.ImageLayer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryEngineTest {

    @Test
    fun testPushAndUndoRedo() {
        val history = HistoryEngine(maxDepth = 5)
        assertFalse(history.canUndo)
        assertFalse(history.canRedo)

        val layer1 = ImageLayer(name = "Capa 1")
        val layer2 = ImageLayer(name = "Capa 2")

        val state0 = HistoryEngine.HistorySnapshot(listOf(layer1), layer1.id, "Inicial")
        val state1 = HistoryEngine.HistorySnapshot(listOf(layer1, layer2), layer2.id, "Añadir Capa 2")

        history.pushSnapshot(state0)
        assertTrue(history.canUndo)
        assertFalse(history.canRedo)

        val restored0 = history.undo(state1)
        assertNotNull(restored0)
        assertEquals(state0.layers.size, restored0!!.layers.size)
        assertFalse(history.canUndo)
        assertTrue(history.canRedo)

        val restored1 = history.redo(restored0)
        assertNotNull(restored1)
        assertEquals(state1.layers.size, restored1!!.layers.size)
        assertTrue(history.canUndo)
        assertFalse(history.canRedo)
    }

    @Test
    fun testMaxDepthEviction() {
        val history = HistoryEngine(maxDepth = 3)
        for (i in 1..5) {
            history.pushSnapshot(
                HistoryEngine.HistorySnapshot(emptyList(), null, "Accion $i")
            )
        }
        // Solo debe haber 3 en la pila
        var count = 0
        var s = history.undo(HistoryEngine.HistorySnapshot(emptyList(), null, "Actual"))
        while (s != null) {
            count++
            s = history.undo(s)
        }
        assertEquals(3, count)
    }
}
