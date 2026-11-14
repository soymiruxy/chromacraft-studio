package com.chromacraft.studio.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.chromacraft.studio.ui.theme.StudioAccent
import com.chromacraft.studio.ui.theme.StudioDark
import com.chromacraft.studio.ui.theme.TextMuted
import com.chromacraft.studio.ui.theme.TextPrimary
import com.chromacraft.studio.ui.theme.TextSecondary
import com.chromacraft.studio.viewmodel.EditorIntent
import com.chromacraft.studio.viewmodel.EditorUiState

/**
 * Diálogo para cambiar la resolución y dimensiones físicas del lienzo.
 */
@Composable
fun ResizeDialog(
    uiState: EditorUiState,
    onIntent: (EditorIntent) -> Unit
) {
    var widthStr by remember { mutableStateOf(uiState.canvasWidth.toString()) }
    var heightStr by remember { mutableStateOf(uiState.canvasHeight.toString()) }

    val presets = listOf(
        Pair("1080 × 1080 (1:1 Cuadrado)", Pair(1080, 1080)),
        Pair("1080 × 1350 (4:5 Retrato)", Pair(1080, 1350)),
        Pair("1080 × 1920 (9:16 Story)", Pair(1080, 1920)),
        Pair("1920 × 1080 (16:9 Portada)", Pair(1920, 1080)),
        Pair("2048 × 2048 (2K HD)", Pair(2048, 2048))
    )

    Dialog(onDismissRequest = { onIntent(EditorIntent.CloseResizeDialog) }) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = StudioDark,
            modifier = Modifier.fillMaxWidth().padding(14.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Dimensiones del Lienzo",
                    fontSize = 18.sp,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Presets rápidos
                Text("Preajustes Estándar", fontSize = 12.sp, color = TextSecondary)
                Spacer(modifier = Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    presets.forEach { (label, dim) ->
                        val isSelected = widthStr == dim.first.toString() && heightStr == dim.second.toString()
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                widthStr = dim.first.toString()
                                heightStr = dim.second.toString()
                            },
                            label = { Text(label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = StudioAccent,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Entradas numéricas manuales
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

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { onIntent(EditorIntent.CloseResizeDialog) }) {
                        Text("Cancelar", color = TextMuted)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val w = widthStr.toIntOrNull() ?: uiState.canvasWidth
                            val h = heightStr.toIntOrNull() ?: uiState.canvasHeight
                            onIntent(EditorIntent.ResizeCanvas(w, h))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StudioAccent),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Aplicar")
                    }
                }
            }
        }
    }
}
