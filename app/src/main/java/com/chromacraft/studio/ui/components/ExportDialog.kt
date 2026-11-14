package com.chromacraft.studio.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.chromacraft.studio.core.graphics.ExportEngine
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
 * Diálogo modal avanzado de exportación con control de formato, resolución, calidad y proyectos editables.
 */
@Composable
fun ExportDialog(
    uiState: EditorUiState,
    onIntent: (EditorIntent) -> Unit
) {
    var format by remember { mutableStateOf(ExportEngine.ExportFormat.PNG) }
    var scale by remember { mutableStateOf(ExportEngine.ResolutionScale.ORIGINAL) }
    var quality by remember { mutableIntStateOf(95) }

    Dialog(onDismissRequest = { onIntent(EditorIntent.CloseExportDialog) }) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = StudioDark,
            modifier = Modifier.fillMaxWidth().padding(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Exportar Composición",
                    fontSize = 18.sp,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Selector de formato
                Text("Formato de Archivo", fontSize = 12.sp, color = TextSecondary)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ExportEngine.ExportFormat.values().forEach { f ->
                        FilterChip(
                            selected = format == f,
                            onClick = { format = f },
                            label = { Text(f.name, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = StudioAccent,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Selector de resolución
                Text("Resolución de Salida", fontSize = 12.sp, color = TextSecondary)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ExportEngine.ResolutionScale.values().forEach { s ->
                        val targetW = (uiState.canvasWidth * s.scale).toInt()
                        val targetH = (uiState.canvasHeight * s.scale).toInt()
                        FilterChip(
                            selected = scale == s,
                            onClick = { scale = s },
                            label = { Text("${targetW}×${targetH}", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = StudioAccent,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                // Control de calidad para JPEG / WebP
                if (format != ExportEngine.ExportFormat.PNG) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Calidad de Compresión", fontSize = 12.sp, color = TextSecondary)
                        Text("$quality%", fontSize = 12.sp, color = TextPrimary)
                    }
                    Slider(
                        value = quality.toFloat(),
                        onValueChange = { quality = it.toInt() },
                        valueRange = 10f..100f,
                        colors = SliderDefaults.colors(thumbColor = StudioAccent, activeTrackColor = StudioAccent)
                    )
                }

                // Transparencia si el formato la soporta
                if (format.supportsTransparency) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Conservar Fondo Transparente", fontSize = 12.sp, color = TextPrimary)
                        Switch(
                            checked = uiState.transparentBackground,
                            onCheckedChange = { onIntent(EditorIntent.ToggleTransparentBackground) },
                            colors = SwitchDefaults.colors(checkedThumbColor = StudioAccent)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Botón para Guardar Proyecto Editable
                OutlinedButton(
                    onClick = {
                        onIntent(EditorIntent.SaveProject)
                        onIntent(EditorIntent.CloseExportDialog)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.BookmarkBorder, contentDescription = "Guardar Proyecto", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Guardar Proyecto Editable (.chroma)", fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Botones Cancelar / Guardar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { onIntent(EditorIntent.CloseExportDialog) }) {
                        Text("Cancelar", color = TextMuted)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onIntent(EditorIntent.Export(format, scale, quality))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StudioAccent),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = "Exportar", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Guardar Imagen")
                    }
                }
            }
        }
    }
}
