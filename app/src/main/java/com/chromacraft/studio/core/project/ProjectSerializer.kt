package com.chromacraft.studio.core.project

import com.chromacraft.studio.core.model.AdjustmentProperties
import com.chromacraft.studio.core.model.BlendModeType
import com.chromacraft.studio.core.model.BrushType
import com.chromacraft.studio.core.model.DrawingLayer
import com.chromacraft.studio.core.model.FilterPreset
import com.chromacraft.studio.core.model.ImageLayer
import com.chromacraft.studio.core.model.Layer
import com.chromacraft.studio.core.model.StickerLayer
import com.chromacraft.studio.core.model.StickerProperties
import com.chromacraft.studio.core.model.StickerShapeType
import com.chromacraft.studio.core.model.StrokePath
import com.chromacraft.studio.core.model.TextLayer
import com.chromacraft.studio.core.model.TextProperties
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Serializador y deserializador de proyectos editables (.chroma).
 * Permite persistir la composición completa (capas, transformaciones, ajustes, textos y vectores)
 * para reanudar la edición en cualquier momento sin pérdida de calidad.
 */
object ProjectSerializer {

    const val PROJECT_VERSION = 1

    fun serializeProject(
        projectName: String,
        width: Int,
        height: Int,
        transparentBg: Boolean,
        layers: List<Layer>
    ): String {
        val root = JSONObject()
        root.put("version", PROJECT_VERSION)
        root.put("name", projectName)
        root.put("width", width)
        root.put("height", height)
        root.put("transparentBg", transparentBg)

        val layersArray = JSONArray()
        for (layer in layers) {
            val lObj = JSONObject()
            lObj.put("id", layer.id)
            lObj.put("name", layer.name)
            lObj.put("isVisible", layer.isVisible)
            lObj.put("isLocked", layer.isLocked)
            lObj.put("opacity", layer.opacity.toDouble())
            lObj.put("blendMode", layer.blendMode.name)
            lObj.put("tx", layer.translationX.toDouble())
            lObj.put("ty", layer.translationY.toDouble())
            lObj.put("sx", layer.scaleX.toDouble())
            lObj.put("sy", layer.scaleY.toDouble())
            lObj.put("rot", layer.rotationDegrees.toDouble())
            lObj.put("flipH", layer.flipHorizontal)
            lObj.put("flipV", layer.flipVertical)

            when (layer) {
                is ImageLayer -> {
                    lObj.put("type", "image")
                    lObj.put("filterPreset", layer.filterPreset.name)
                    val adj = layer.adjustments
                    val aObj = JSONObject().apply {
                        put("brightness", adj.brightness.toDouble())
                        put("contrast", adj.contrast.toDouble())
                        put("saturation", adj.saturation.toDouble())
                        put("temperature", adj.temperature.toDouble())
                        put("tint", adj.tint.toDouble())
                        put("exposure", adj.exposure.toDouble())
                        put("shadows", adj.shadows.toDouble())
                        put("highlights", adj.highlights.toDouble())
                        put("vignette", adj.vignette.toDouble())
                    }
                    lObj.put("adjustments", aObj)
                }

                is DrawingLayer -> {
                    lObj.put("type", "drawing")
                    val strokesArray = JSONArray()
                    for (s in layer.strokes) {
                        val sObj = JSONObject().apply {
                            put("color", s.color)
                            put("width", s.strokeWidth.toDouble())
                            put("opacity", s.opacity.toDouble())
                            put("isEraser", s.isEraser)
                            put("brushType", s.brushType.name)
                            val pts = JSONArray()
                            s.points.forEach { p ->
                                pts.put(JSONObject().apply {
                                    put("x", p.x.toDouble())
                                    put("y", p.y.toDouble())
                                })
                            }
                            put("points", pts)
                        }
                        strokesArray.put(sObj)
                    }
                    lObj.put("strokes", strokesArray)
                }

                is TextLayer -> {
                    lObj.put("type", "text")
                    val p = layer.textProperties
                    val tObj = JSONObject().apply {
                        put("text", p.text)
                        put("fontSize", p.fontSize.toDouble())
                        put("textColor", p.textColor)
                        put("isBold", p.isBold)
                        put("isItalic", p.isItalic)
                        put("strokeWidth", p.strokeWidth.toDouble())
                        put("shadowRadius", p.shadowRadius.toDouble())
                    }
                    lObj.put("textProperties", tObj)
                }

                is StickerLayer -> {
                    lObj.put("type", "sticker")
                    val s = layer.stickerProperties
                    val sObj = JSONObject().apply {
                        put("shapeType", s.shapeType.name)
                        put("fillColor", s.fillColor)
                        put("strokeColor", s.strokeColor)
                        put("strokeWidth", s.strokeWidth.toDouble())
                    }
                    lObj.put("stickerProperties", sObj)
                }
            }
            layersArray.put(lObj)
        }
        root.put("layers", layersArray)

        return root.toString(2)
    }

    fun saveProjectToFile(file: File, json: String): Boolean {
        return try {
            file.writeText(json)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
