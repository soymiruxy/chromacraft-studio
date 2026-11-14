package com.chromacraft.studio.ui.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import com.chromacraft.studio.ui.theme.StudioAccent
import com.chromacraft.studio.ui.theme.StudioDark
import com.chromacraft.studio.ui.theme.TextMuted
import com.chromacraft.studio.ui.theme.TextPrimary
import com.chromacraft.studio.ui.theme.TextSecondary

enum class CanvasBgColor(val label: String, val colorLong: Long, val isTransparent: Boolean) {
    STUDIO_DARK("Oscuro", 0xFF0D0E12, false),
    WHITE("Blanco", 0xFFFFFFFF, false),
    TRANSPARENT("Transparente", 0x00000000, true)
}

/**
 * Diálogo modal para la creación de un nuevo proyecto desde lienzo en blanco.
 */
@Composable
fun NewCanvasDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, width: Int, height: Int, transparentBg: Boolean, bgColor: Long) -> Unit
) {
    var projectName by remember { mutableStateOf("Mi Creación") }
    var widthStr by remember { mutableStateOf("1080") }
    var heightStr by remember { mutableStateOf("1080") }
    var selectedBg by remember { mutableStateOf(CanvasBgColor.STUDIO_DARK) }

    val presets = listOf(
        Pair("1:1 Cuadrado", Pair(1080, 1080)),
        Pair("4:5 Post", Pair(1080, 1350)),
        Pair("9:16 Story", Pair(1080, 1920)),
        Pair("16:9 Portada", Pair(1920, 1080))
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = StudioDark,
            modifier = Modifier.fillMaxWidth().padding(12.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Nuevo Lienzo en Blanco",
                    fontSize = 18.sp,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = projectName,
                    onValueChange = { projectName = it },
                    label = { Text("Nombre del proyecto") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = StudioAccent,
                        focusedLabelColor = StudioAccent,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text("Plantillas Rápidas", fontSize = 12.sp, color = TextSecondary)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presets.forEach { (label, dim) ->
                        val isSelected = widthStr == dim.first.toString() && heightStr == dim.second.toString()
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                widthStr = dim.first.toString()
                                heightStr = dim.second.toString()
                            },
                            label = { Text(label, fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = StudioAccent,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = widthStr,
                        onValueChange = { widthStr = it.filter { c -> c.isDigit() } },
                        label = { Text("Ancho (px)") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = StudioAccent,
                            focusedLabelColor = StudioAccent,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    OutlinedTextField(
                        value = heightStr,
                        onValueChange = { heightStr = it.filter { c -> c.isDigit() } },
                        label = { Text("Alto (px)") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = StudioAccent,
                            focusedLabelColor = StudioAccent,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text("Fondo del Lienzo", fontSize = 12.sp, color = TextSecondary)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CanvasBgColor.values().forEach { bg ->
                        FilterChip(
                            selected = selectedBg == bg,
                            onClick = { selectedBg = bg },
                            label = { Text(bg.label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = StudioAccent,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar", color = TextMuted)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val w = widthStr.toIntOrNull()?.coerceIn(200, 8192) ?: 1080
                            val h = heightStr.toIntOrNull()?.coerceIn(200, 8192) ?: 1080
                            onCreate(projectName.ifBlank { "Lienzo" }, w, h, selectedBg.isTransparent, selectedBg.colorLong)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StudioAccent),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Crear Lienzo")
                    }
                }
            }
        }
    }
}
