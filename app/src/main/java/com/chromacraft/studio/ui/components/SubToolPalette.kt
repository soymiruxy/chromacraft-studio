package com.chromacraft.studio.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Rotate90DegreesCcw
import androidx.compose.material.icons.filled.Rotate90DegreesCw
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chromacraft.studio.core.model.BrushType
import com.chromacraft.studio.core.model.CropRatio
import com.chromacraft.studio.core.model.CurveChannel
import com.chromacraft.studio.core.model.CurvePoint
import com.chromacraft.studio.core.model.FilterPreset
import com.chromacraft.studio.core.model.StickerProperties
import com.chromacraft.studio.core.model.StickerShapeType
import com.chromacraft.studio.core.model.ToolType
import com.chromacraft.studio.ui.theme.StudioAccent
import com.chromacraft.studio.ui.theme.StudioSurface
import com.chromacraft.studio.ui.theme.StudioSurfaceVariant
import com.chromacraft.studio.ui.theme.TextMuted
import com.chromacraft.studio.ui.theme.TextPrimary
import com.chromacraft.studio.ui.theme.TextSecondary
import com.chromacraft.studio.viewmodel.EditorIntent
import com.chromacraft.studio.viewmodel.EditorUiState

/**
 * Paleta secundaria contextual que aloja los controles específicos de la herramienta activa.
 */
@Composable
fun SubToolPalette(
    uiState: EditorUiState,
    onIntent: (EditorIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(148.dp),
        color = StudioSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            // Cabecera con título de la herramienta y botones Cerrar / Aplicar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = uiState.activeTool.label.uppercase(),
                    color = TextSecondary,
                    fontSize = 11.sp
                )

                Row {
                    IconButton(
                        onClick = { onIntent(EditorIntent.CloseTool) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Cancelar", tint = TextMuted, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = { onIntent(EditorIntent.CloseTool) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Aplicar", tint = StudioAccent, modifier = Modifier.size(18.dp))
                    }
                }
            }

            // Cuerpo dinámico según herramienta activa
            when (uiState.activeTool) {
                ToolType.CROP -> CropSubPalette(uiState, onIntent)
                ToolType.ADJUST -> AdjustSubPalette(uiState, onIntent)
                ToolType.CURVES -> CurvesSubPalette(uiState, onIntent)
                ToolType.FILTER -> FilterSubPalette(uiState, onIntent)
                ToolType.BLUR_SHARPEN -> OpticsSubPalette(uiState, onIntent)
                ToolType.CUTOUT -> CutoutSubPalette(uiState, onIntent)
                ToolType.DRAW -> DrawSubPalette(uiState, onIntent)
                ToolType.STICKER -> StickerSubPalette(onIntent)
                else -> Unit
            }
        }
    }
}

@Composable
private fun CropSubPalette(uiState: EditorUiState, onIntent: (EditorIntent) -> Unit) {
    Column {
        // Acciones de rotación de 90° y volteo especular
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onIntent(EditorIntent.Rotate90(false)) }) {
                Icon(Icons.Default.Rotate90DegreesCcw, contentDescription = "Rotar -90°", tint = TextPrimary)
            }
            IconButton(onClick = { onIntent(EditorIntent.Rotate90(true)) }) {
                Icon(Icons.Default.Rotate90DegreesCw, contentDescription = "Rotar +90°", tint = TextPrimary)
            }
            val sel = uiState.selectedLayer
            if (sel != null) {
                IconButton(onClick = { onIntent(EditorIntent.FlipLayerHorizontal(sel.id)) }) {
                    Icon(Icons.Default.Flip, contentDescription = "Voltear H", tint = TextPrimary)
                }
            }

            // Ratios de recorte
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CropRatio.values().forEach { ratio ->
                    FilterChip(
                        selected = uiState.cropRatio == ratio,
                        onClick = { onIntent(EditorIntent.SetCropRatio(ratio)) },
                        label = { Text(ratio.label, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = StudioAccent, selectedLabelColor = Color.White)
                    )
                }
            }
        }
    }
}

@Composable
private fun AdjustSubPalette(uiState: EditorUiState, onIntent: (EditorIntent) -> Unit) {
    var activeCategory by remember { mutableStateOf("Brillo") }
    val adjustments = uiState.currentAdjustments

    val categories = listOf("Brillo", "Contraste", "Saturación", "Exposición", "Sombras", "Luces", "Temperatura", "Tinte")

    Column {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            categories.forEach { cat ->
                FilterChip(
                    selected = activeCategory == cat,
                    onClick = { activeCategory = cat },
                    label = { Text(cat, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = StudioAccent, selectedLabelColor = Color.White)
                )
            }
        }

        val (currentValue, onValueChange) = when (activeCategory) {
            "Brillo" -> adjustments.brightness to { v: Float -> onIntent(EditorIntent.UpdateAdjustments(adjustments.copy(brightness = v))) }
            "Contraste" -> adjustments.contrast to { v: Float -> onIntent(EditorIntent.UpdateAdjustments(adjustments.copy(contrast = v))) }
            "Saturación" -> adjustments.saturation to { v: Float -> onIntent(EditorIntent.UpdateAdjustments(adjustments.copy(saturation = v))) }
            "Exposición" -> adjustments.exposure to { v: Float -> onIntent(EditorIntent.UpdateAdjustments(adjustments.copy(exposure = v))) }
            "Sombras" -> adjustments.shadows to { v: Float -> onIntent(EditorIntent.UpdateAdjustments(adjustments.copy(shadows = v))) }
            "Luces" -> adjustments.highlights to { v: Float -> onIntent(EditorIntent.UpdateAdjustments(adjustments.copy(highlights = v))) }
            "Temperatura" -> adjustments.temperature to { v: Float -> onIntent(EditorIntent.UpdateAdjustments(adjustments.copy(temperature = v))) }
            "Tinte" -> adjustments.tint to { v: Float -> onIntent(EditorIntent.UpdateAdjustments(adjustments.copy(tint = v))) }
            else -> 0f to {}
        }

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
            Slider(
                value = currentValue,
                onValueChange = onValueChange,
                valueRange = -100f..100f,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(thumbColor = StudioAccent, activeTrackColor = StudioAccent)
            )
            Text("${currentValue.toInt()}", color = TextPrimary, fontSize = 12.sp, modifier = Modifier.width(36.dp))
        }
    }
}

@Composable
private fun CurvesSubPalette(uiState: EditorUiState, onIntent: (EditorIntent) -> Unit) {
    val curves = uiState.currentToneCurves
    val activeChannel = uiState.activeCurveChannel

    Column {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CurveChannel.values().forEach { ch ->
                FilterChip(
                    selected = activeChannel == ch,
                    onClick = { onIntent(EditorIntent.SetActiveCurveChannel(ch)) },
                    label = { Text(ch.displayName, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = StudioAccent, selectedLabelColor = Color.White)
                )
            }
        }

        // Control de Gamma de Niveles
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Gamma: ${"%.2f".format(curves.levels.gamma)}", fontSize = 11.sp, color = TextSecondary)
            Slider(
                value = curves.levels.gamma,
                onValueChange = { g ->
                    onIntent(EditorIntent.UpdateToneCurves(curves.copy(levels = curves.levels.copy(gamma = g))))
                },
                valueRange = 0.3f..2.5f,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                colors = SliderDefaults.colors(thumbColor = StudioAccent, activeTrackColor = StudioAccent)
            )
        }
    }
}

@Composable
private fun OpticsSubPalette(uiState: EditorUiState, onIntent: (EditorIntent) -> Unit) {
    val adjustments = uiState.currentAdjustments
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Desenfoque", fontSize = 11.sp, color = TextSecondary, modifier = Modifier.width(76.dp))
            Slider(
                value = adjustments.blurRadius,
                onValueChange = { onIntent(EditorIntent.UpdateAdjustments(adjustments.copy(blurRadius = it))) },
                valueRange = 0f..40f,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(thumbColor = StudioAccent, activeTrackColor = StudioAccent)
            )
            Text("${adjustments.blurRadius.toInt()}px", fontSize = 11.sp, color = TextPrimary)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Nitidez", fontSize = 11.sp, color = TextSecondary, modifier = Modifier.width(76.dp))
            Slider(
                value = adjustments.sharpen,
                onValueChange = { onIntent(EditorIntent.UpdateAdjustments(adjustments.copy(sharpen = it))) },
                valueRange = 0f..100f,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(thumbColor = StudioAccent, activeTrackColor = StudioAccent)
            )
            Text("${adjustments.sharpen.toInt()}%", fontSize = 11.sp, color = TextPrimary)
        }
    }
}

@Composable
private fun CutoutSubPalette(uiState: EditorUiState, onIntent: (EditorIntent) -> Unit) {
    Column {
        Text("Dibuja una silueta alrededor del objeto sobre el lienzo:", fontSize = 11.sp, color = TextSecondary)
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { onIntent(EditorIntent.FinishLassoCutout(invert = false)) },
                enabled = uiState.lassoPoints.size >= 3,
                colors = ButtonDefaults.buttonColors(containerColor = StudioAccent)
            ) {
                Text("Recortar Objeto", fontSize = 11.sp)
            }

            Button(
                onClick = { onIntent(EditorIntent.FinishLassoCutout(invert = true)) },
                enabled = uiState.lassoPoints.size >= 3,
                colors = ButtonDefaults.buttonColors(containerColor = StudioSurfaceVariant)
            ) {
                Text("Invertir Máscara", fontSize = 11.sp)
            }

            Button(
                onClick = { onIntent(EditorIntent.ClearLasso) },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
            ) {
                Text("Limpiar", color = TextMuted, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun FilterSubPalette(uiState: EditorUiState, onIntent: (EditorIntent) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterPreset.values().forEach { preset ->
            val isSelected = uiState.currentFilterPreset == preset
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) StudioAccent else StudioSurfaceVariant)
                    .clickable { onIntent(EditorIntent.SelectFilterPreset(preset)) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(preset.displayName, fontSize = 11.sp, color = if (isSelected) Color.White else TextPrimary)
                    Text(preset.category, fontSize = 9.sp, color = if (isSelected) Color(0xFFE0E0E0) else TextMuted)
                }
            }
        }
    }
}

@Composable
private fun DrawSubPalette(uiState: EditorUiState, onIntent: (EditorIntent) -> Unit) {
    val brush = uiState.brushProperties
    val colorPalette = listOf(0xFFFFFFFF, 0xFF000000, 0xFFFF5252, 0xFFFFD700, 0xFF00E676, 0xFF00E5FF, 0xFF7C4DFF, 0xFFFF4081)

    Column {
        // Selector de tipo de pincel
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            BrushType.values().forEach { bt ->
                FilterChip(
                    selected = brush.brushType == bt,
                    onClick = { onIntent(EditorIntent.UpdateBrushProperties(brush.copy(brushType = bt, isEraser = false))) },
                    label = { Text(bt.displayName, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = StudioAccent, selectedLabelColor = Color.White)
                )
            }
            FilterChip(
                selected = brush.isEraser,
                onClick = { onIntent(EditorIntent.UpdateBrushProperties(brush.copy(isEraser = !brush.isEraser))) },
                label = { Text("Borrador", fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = StudioAccent, selectedLabelColor = Color.White)
            )
        }

        // Colores y Slider de grosor
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                colorPalette.take(5).forEach { c ->
                    val isSelected = brush.color == c && !brush.isEraser
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(c))
                            .border(width = if (isSelected) 3.dp else 1.dp, color = if (isSelected) StudioAccent else Color(0x33FFFFFF), shape = CircleShape)
                            .clickable { onIntent(EditorIntent.UpdateBrushProperties(brush.copy(color = c, isEraser = false))) }
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Slider(
                value = brush.size,
                onValueChange = { onIntent(EditorIntent.UpdateBrushProperties(brush.copy(size = it))) },
                valueRange = 4f..80f,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(thumbColor = StudioAccent, activeTrackColor = StudioAccent)
            )
            Text("${brush.size.toInt()}px", color = TextPrimary, fontSize = 11.sp, modifier = Modifier.width(36.dp))
        }
    }
}

@Composable
private fun StickerSubPalette(onIntent: (EditorIntent) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StickerShapeType.values().forEach { shape ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(StudioSurfaceVariant)
                    .clickable { onIntent(EditorIntent.AddStickerLayer(StickerProperties(shapeType = shape))) }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(shape.label, fontSize = 12.sp, color = TextPrimary)
            }
        }
    }
}
