package com.chromacraft.studio.core.graphics

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import com.chromacraft.studio.core.model.AdjustmentProperties
import com.chromacraft.studio.core.model.BlendModeType
import com.chromacraft.studio.core.model.FilterPreset

/**
 * Motor de procesamiento cromático y filtros por GPU mediante matrices de color 4x5.
 * Concatena ajustes continuos y preajustes estilísticos en una sola pasada de sombreado.
 */
object FilterEngine {

    /**
     * Mapea el tipo de fusión de alto nivel a un modo PorterDuff nativo de Android.
     */
    fun toPorterDuffMode(blendMode: BlendModeType): PorterDuff.Mode {
        return when (blendMode) {
            BlendModeType.NORMAL -> PorterDuff.Mode.SRC_OVER
            BlendModeType.MULTIPLY -> PorterDuff.Mode.MULTIPLY
            BlendModeType.SCREEN -> PorterDuff.Mode.SCREEN
            BlendModeType.OVERLAY -> PorterDuff.Mode.OVERLAY
            BlendModeType.DARKEN -> PorterDuff.Mode.DARKEN
            BlendModeType.LIGHTEN -> PorterDuff.Mode.LIGHTEN
            BlendModeType.ADD -> PorterDuff.Mode.ADD
            BlendModeType.DIFFERENCE -> PorterDuff.Mode.XOR
            BlendModeType.SOFT_LIGHT -> PorterDuff.Mode.SRC_OVER
            BlendModeType.COLOR_DODGE -> PorterDuff.Mode.ADD // Aproximación segura para PorterDuff
        }
    }

    fun toXfermode(blendMode: BlendModeType): PorterDuffXfermode {
        return PorterDuffXfermode(toPorterDuffMode(blendMode))
    }

    /**
     * Compone la matriz completa de color a partir de los ajustes del usuario y el preset activo.
     */
    fun buildColorMatrix(
        adjustments: AdjustmentProperties,
        preset: FilterPreset
    ): ColorMatrix {
        val master = ColorMatrix()

        // 1. Aplicar matriz base del preajuste estilístico
        val presetMatrix = getPresetMatrix(preset)
        master.postConcat(presetMatrix)

        // 2. Aplicar Brillo
        if (adjustments.brightness != 0f) {
            val b = adjustments.brightness * 2.55f // Escala -255 a +255
            val brightnessMatrix = ColorMatrix(floatArrayOf(
                1f, 0f, 0f, 0f, b,
                0f, 1f, 0f, 0f, b,
                0f, 0f, 1f, 0f, b,
                0f, 0f, 0f, 1f, 0f
            ))
            master.postConcat(brightnessMatrix)
        }

        // 3. Aplicar Contraste
        if (adjustments.contrast != 0f) {
            val c = (100f + adjustments.contrast) / 100f
            val scale = c * c
            val translate = (-0.5f * scale + 0.5f) * 255f
            val contrastMatrix = ColorMatrix(floatArrayOf(
                scale, 0f, 0f, 0f, translate,
                0f, scale, 0f, 0f, translate,
                0f, 0f, scale, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            ))
            master.postConcat(contrastMatrix)
        }

        // 4. Aplicar Saturación
        if (adjustments.saturation != 0f) {
            val satMatrix = ColorMatrix()
            val satValue = (100f + adjustments.saturation) / 100f
            satMatrix.setSaturation(satValue.coerceAtLeast(0f))
            master.postConcat(satMatrix)
        }

        // 5. Aplicar Temperatura de Color (Kelvin)
        if (adjustments.temperature != 0f) {
            val temp = adjustments.temperature / 100f
            val rShift = if (temp > 0) temp * 40f else 0f
            val bShift = if (temp < 0) -temp * 40f else 0f
            val tempMatrix = ColorMatrix(floatArrayOf(
                1f, 0f, 0f, 0f, rShift,
                0f, 1f, 0f, 0f, 0f,
                0f, 0f, 1f, 0f, bShift,
                0f, 0f, 0f, 1f, 0f
            ))
            master.postConcat(tempMatrix)
        }

        // 6. Aplicar Tinte (Verde - Magenta)
        if (adjustments.tint != 0f) {
            val tint = adjustments.tint / 100f
            val gShift = if (tint < 0) -tint * 30f else 0f
            val rShift = if (tint > 0) tint * 20f else 0f
            val bShift = if (tint > 0) tint * 20f else 0f
            val tintMatrix = ColorMatrix(floatArrayOf(
                1f, 0f, 0f, 0f, rShift,
                0f, 1f, 0f, 0f, gShift,
                0f, 0f, 1f, 0f, bShift,
                0f, 0f, 0f, 1f, 0f
            ))
            master.postConcat(tintMatrix)
        }

        // 7. Exposición
        if (adjustments.exposure != 0f) {
            val exp = (100f + adjustments.exposure) / 100f
            val expMatrix = ColorMatrix(floatArrayOf(
                exp, 0f, 0f, 0f, 0f,
                0f, exp, 0f, 0f, 0f,
                0f, 0f, exp, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            ))
            master.postConcat(expMatrix)
        }

        // 8. Sombras y Altas Luces
        if (adjustments.shadows != 0f || adjustments.highlights != 0f) {
            val s = adjustments.shadows * 0.4f
            val h = adjustments.highlights * 0.4f
            val shMatrix = ColorMatrix(floatArrayOf(
                1f + (h * 0.005f), 0f, 0f, 0f, s,
                0f, 1f + (h * 0.005f), 0f, 0f, s,
                0f, 0f, 1f + (h * 0.005f), 0f, s,
                0f, 0f, 0f, 1f, 0f
            ))
            master.postConcat(shMatrix)
        }

        return master
    }

    fun createColorFilter(
        adjustments: AdjustmentProperties,
        preset: FilterPreset
    ): ColorMatrixColorFilter {
        return ColorMatrixColorFilter(buildColorMatrix(adjustments, preset))
    }

    private fun getPresetMatrix(preset: FilterPreset): ColorMatrix {
        val matrix = ColorMatrix()
        when (preset) {
            FilterPreset.NONE -> Unit

            FilterPreset.VIVID -> {
                matrix.setSaturation(1.4f)
                val boost = ColorMatrix(floatArrayOf(
                    1.1f, 0f, 0f, 0f, 5f,
                    0f, 1.05f, 0f, 0f, 5f,
                    0f, 0f, 1.15f, 0f, 5f,
                    0f, 0f, 0f, 1f, 0f
                ))
                matrix.postConcat(boost)
            }

            FilterPreset.WARM -> {
                matrix.set(floatArrayOf(
                    1.2f, 0f, 0f, 0f, 20f,
                    0f, 1.05f, 0f, 0f, 10f,
                    0f, 0f, 0.85f, 0f, -15f,
                    0f, 0f, 0f, 1f, 0f
                ))
            }

            FilterPreset.COOL -> {
                matrix.set(floatArrayOf(
                    0.85f, 0f, 0f, 0f, -10f,
                    0f, 1.0f, 0f, 0f, 5f,
                    0f, 0f, 1.25f, 0f, 25f,
                    0f, 0f, 0f, 1f, 0f
                ))
            }

            FilterPreset.DRAMA -> {
                matrix.setSaturation(0.85f)
                val dramaContrast = ColorMatrix(floatArrayOf(
                    1.4f, 0f, 0f, 0f, -40f,
                    0f, 1.4f, 0f, 0f, -40f,
                    0f, 0f, 1.4f, 0f, -40f,
                    0f, 0f, 0f, 1f, 0f
                ))
                matrix.postConcat(dramaContrast)
            }

            FilterPreset.MONO -> {
                matrix.setSaturation(0f)
                val highContrast = ColorMatrix(floatArrayOf(
                    1.2f, 0f, 0f, 0f, -20f,
                    0f, 1.2f, 0f, 0f, -20f,
                    0f, 0f, 1.2f, 0f, -20f,
                    0f, 0f, 0f, 1f, 0f
                ))
                matrix.postConcat(highContrast)
            }

            FilterPreset.SEPIA -> {
                matrix.set(floatArrayOf(
                    0.393f, 0.769f, 0.189f, 0f, 0f,
                    0.349f, 0.686f, 0.168f, 0f, 0f,
                    0.272f, 0.534f, 0.131f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                ))
            }

            FilterPreset.CYBERPUNK -> {
                matrix.set(floatArrayOf(
                    1.3f, 0f, 0.2f, 0f, 30f,
                    0f, 0.9f, 0.4f, 0f, -10f,
                    0.4f, 0f, 1.5f, 0f, 40f,
                    0f, 0f, 0f, 1f, 0f
                ))
            }

            FilterPreset.VINTAGE -> {
                matrix.set(floatArrayOf(
                    0.9f, 0f, 0.1f, 0f, 25f,
                    0f, 0.85f, 0.1f, 0f, 15f,
                    0.1f, 0.1f, 0.7f, 0f, 30f,
                    0f, 0f, 0f, 1f, 0f
                ))
            }

            FilterPreset.DUOTONE -> {
                // Tinte azul en sombras y naranja/magenta en luces
                matrix.set(floatArrayOf(
                    1.4f, 0f, 0f, 0f, 20f,
                    0.2f, 0.8f, 0f, 0f, -10f,
                    0f, 0.3f, 1.5f, 0f, 40f,
                    0f, 0f, 0f, 1f, 0f
                ))
            }

            FilterPreset.INVERT -> {
                matrix.set(floatArrayOf(
                    -1f, 0f, 0f, 0f, 255f,
                    0f, -1f, 0f, 0f, 255f,
                    0f, 0f, -1f, 0f, 255f,
                    0f, 0f, 0f, 1f, 0f
                ))
            }
        }
        return matrix
    }
}
