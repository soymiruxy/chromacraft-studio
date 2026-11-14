package com.chromacraft.studio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.InvertColors
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import com.chromacraft.studio.core.model.BlendModeType
import com.chromacraft.studio.core.model.Layer
import com.chromacraft.studio.ui.theme.StudioAccent
import com.chromacraft.studio.ui.theme.StudioDark
import com.chromacraft.studio.ui.theme.StudioSurface
import com.chromacraft.studio.ui.theme.StudioSurfaceVariant
import com.chromacraft.studio.ui.theme.TextMuted
import com.chromacraft.studio.ui.theme.TextPrimary
import com.chromacraft.studio.ui.theme.TextSecondary
import com.chromacraft.studio.viewmodel.EditorIntent
import com.chromacraft.studio.viewmodel.EditorUiState

/**
 * Gestor de capas profesional: permite reordenamiento vertical, alternar visibilidad,
 * control granular de opacidad, máscaras y selección de modos de fusión PorterDuff.
 */
@Composable
fun LayerManagerContent(
    uiState: EditorUiState,
    onIntent: (EditorIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddMenu by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = StudioDark
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Cabecera del Gestor de Capas
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CAPAS & MÁSCARAS (${uiState.layers.size})",
                    fontSize = 13.sp,
                    color = TextPrimary
                )

                Box {
                    Button(
                        onClick = { showAddMenu = true },
                        colors = ButtonDefaults.buttonColors(containerColor = StudioAccent),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = ButtonDefaults.ButtonWithIconContentPadding
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Añadir", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Añadir", fontSize = 12.sp)
                    }

                    DropdownMenu(
                        expanded = showAddMenu,
                        onDismissRequest = { showAddMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Capa de Dibujo") },
                            onClick = {
                                onIntent(EditorIntent.AddDrawingLayer)
                                showAddMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Capa de Texto") },
                            onClick = {
                                onIntent(EditorIntent.OpenTextEditor())
                                showAddMenu = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            val reversedLayers = uiState.layers.asReversed()

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                itemsIndexed(reversedLayers) { index, layer ->
                    val actualIndex = uiState.layers.size - 1 - index
                    val isSelected = uiState.selectedLayerId == layer.id

                    LayerCardItem(
                        layer = layer,
                        isSelected = isSelected,
                        canMoveUp = actualIndex < uiState.layers.size - 1,
                        canMoveDown = actualIndex > 0,
                        onSelect = { onIntent(EditorIntent.SelectLayer(layer.id)) },
                        onToggleVisibility = { onIntent(EditorIntent.UpdateLayerVisibility(layer.id, !layer.isVisible)) },
                        onMoveUp = { onIntent(EditorIntent.ReorderLayers(actualIndex, actualIndex + 1)) },
                        onMoveDown = { onIntent(EditorIntent.ReorderLayers(actualIndex, actualIndex - 1)) },
                        onDuplicate = { onIntent(EditorIntent.DuplicateLayer(layer.id)) },
                        onDelete = { onIntent(EditorIntent.DeleteLayer(layer.id)) },
                        onOpacityChange = { onIntent(EditorIntent.UpdateLayerOpacity(layer.id, it)) },
                        onBlendModeChange = { onIntent(EditorIntent.UpdateLayerBlendMode(layer.id, it)) },
                        onToggleMask = { onIntent(EditorIntent.ToggleLayerMask(layer.id)) },
                        onInvertMask = { onIntent(EditorIntent.InvertLayerMask(layer.id)) }
                    )
                }
            }
        }
    }
}

@Composable
private fun LayerCardItem(
    layer: Layer,
    isSelected: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onSelect: () -> Unit,
    onToggleVisibility: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onOpacityChange: (Float) -> Unit,
    onBlendModeChange: (BlendModeType) -> Unit,
    onToggleMask: () -> Unit,
    onInvertMask: () -> Unit
) {
    var showBlendMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) StudioSurfaceVariant else StudioSurface)
            .border(
                width = if (isSelected) 1.5.dp else 0.5.dp,
                color = if (isSelected) StudioAccent else Color(0x22FFFFFF),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onSelect)
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onToggleVisibility, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = if (layer.isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = "Visibilidad",
                        tint = if (layer.isVisible) StudioAccent else TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Column {
                    Text(text = layer.name, fontSize = 13.sp, color = TextPrimary)
                    Text(
                        text = "Fusión: ${layer.blendMode.displayName}",
                        fontSize = 10.sp,
                        color = TextSecondary,
                        modifier = Modifier.clickable { showBlendMenu = true }
                    )

                    DropdownMenu(
                        expanded = showBlendMenu,
                        onDismissRequest = { showBlendMenu = false }
                    ) {
                        BlendModeType.values().forEach { mode ->
                            DropdownMenuItem(
                                text = { Text(mode.displayName) },
                                onClick = {
                                    onBlendModeChange(mode)
                                    showBlendMenu = false
                                }
                            )
                        }
                    }
                }
            }

            // Acciones: Duplicar, Subir, Bajar, Eliminar
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDuplicate, modifier = Modifier.size(26.dp)) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Duplicar", tint = TextPrimary, modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onMoveUp, enabled = canMoveUp, modifier = Modifier.size(26.dp)) {
                    Icon(Icons.Default.ArrowUpward, contentDescription = "Subir", tint = if (canMoveUp) TextPrimary else TextMuted, modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onMoveDown, enabled = canMoveDown, modifier = Modifier.size(26.dp)) {
                    Icon(Icons.Default.ArrowDownward, contentDescription = "Bajar", tint = if (canMoveDown) TextPrimary else TextMuted, modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(26.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color(0xFFFF5252), modifier = Modifier.size(16.dp))
                }
            }
        }

        // Estado de Máscara de Capa si existe
        if (layer.mask != null) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (layer.mask!!.isEnabled) "🎭 Máscara Activa" else "🎭 Máscara Desactivada",
                    fontSize = 10.sp,
                    color = if (layer.mask!!.isEnabled) StudioAccent else TextMuted,
                    modifier = Modifier.clickable { onToggleMask() }
                )
                Text(
                    text = "Invertir",
                    fontSize = 10.sp,
                    color = StudioAccent,
                    modifier = Modifier.clickable { onInvertMask() }
                )
            }
        }

        // Control de Opacidad si está seleccionada
        if (isSelected) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Text("Opacidad", fontSize = 10.sp, color = TextSecondary)
                Slider(
                    value = layer.opacity,
                    onValueChange = onOpacityChange,
                    valueRange = 0f..1f,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                    colors = SliderDefaults.colors(thumbColor = StudioAccent, activeTrackColor = StudioAccent)
                )
                Text("${(layer.opacity * 100).toInt()}%", fontSize = 10.sp, color = TextPrimary)
            }
        }
    }
}
