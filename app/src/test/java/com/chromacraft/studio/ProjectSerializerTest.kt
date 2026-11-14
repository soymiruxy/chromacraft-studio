package com.chromacraft.studio

import com.chromacraft.studio.core.model.ImageLayer
import com.chromacraft.studio.core.model.StickerLayer
import com.chromacraft.studio.core.model.StickerProperties
import com.chromacraft.studio.core.model.StickerShapeType
import com.chromacraft.studio.core.model.TextLayer
import com.chromacraft.studio.core.model.TextProperties
import com.chromacraft.studio.core.project.ProjectSerializer
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectSerializerTest {

    @Test
    fun testSerializeProjectProducesValidJson() {
        val baseLayer = ImageLayer(name = "Fondo")
        val textLayer = TextLayer(name = "Texto Titulo", textProperties = TextProperties(text = "Hola Mundo"))
        val stickerLayer = StickerLayer(name = "Estrella", stickerProperties = StickerProperties(shapeType = StickerShapeType.STAR))

        val layers = listOf(baseLayer, textLayer, stickerLayer)
        val jsonString = ProjectSerializer.serializeProject(
            projectName = "TestProject",
            width = 1920,
            height = 1080,
            transparentBg = true,
            layers = layers
        )

        assertNotNull(jsonString)
        val json = JSONObject(jsonString)
        assertEquals("TestProject", json.getString("name"))
        assertEquals(1920, json.getInt("width"))
        assertEquals(1080, json.getInt("height"))
        assertTrue(json.getBoolean("transparentBg"))

        val layersArray = json.getJSONArray("layers")
        assertEquals(3, layersArray.length())
        assertEquals("image", layersArray.getJSONObject(0).getString("type"))
        assertEquals("text", layersArray.getJSONObject(1).getString("type"))
        assertEquals("sticker", layersArray.getJSONObject(2).getString("type"))
    }
}
