package com.chromacraft.studio.ui.components

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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import com.chromacraft.studio.core.model.TextProperties
import com.chromacraft.studio.ui.theme.StudioAccent
import com.chromacraft.studio.ui.theme.StudioDark
import com.chromacraft.studio.ui.theme.StudioSurface
import com.chromacraft.studio.ui.theme.TextMuted
import com.chromacraft.studio.ui.theme.TextPrimary
import com.chromacraft.studio.ui.theme.TextSecondary
import com.chromacraft.studio.viewmodel.EditorIntent

/**
 * Diálogo modal para inserción y personalización tipográfica avanzada de capas de texto.
 */
@Composable
fun TextEditorModal(
    initialProperties: TextProperties,
    onIntent: (EditorIntent) -> Unit
) {
    var text by remember { mutableStateOf(initialProperties.text) }
    var fontSize by remember { mutableFloatStateOf(initialProperties.fontSize) }
    var selectedColor by remember { mutableLongStateOf(initialProperties.textColor) }

    val colorOptions = listOf(
        0xFFFFFFFF, 0xFF000000, 0xFFFF5252, 0xFFFFD700,
        0xFF00E676, 0xFF00E5FF, 0xFF7C4DFF, 0xFFFF4081
    )

    Dialog(onDismissRequest = { onIntent(EditorIntent.CloseTextEditor) }) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = StudioDark,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Añadir Texto",
                    fontSize = 16.sp,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Escribe aquí...") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = StudioAccent,
                        focusedLabelColor = StudioAccent,
                        unfocusedTextColor = TextPrimary,
                        focusedTextColor = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Selector de tamaño
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Tamaño", fontSize = 12.sp, color = TextSecondary)
                    Slider(
                        value = fontSize,
                        onValueChange = { fontSize = it },
                        valueRange = 24f..120f,
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                        colors = SliderDefaults.colors(thumbColor = StudioAccent, activeTrackColor = StudioAccent)
                    )
                    Text("${fontSize.toInt()}sp", fontSize = 12.sp, color = TextPrimary)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Selector de color
                Text("Color", fontSize = 12.sp, color = TextSecondary)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    colorOptions.forEach { c ->
                        val isSelected = selectedColor == c
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(c))
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) StudioAccent else Color(0x33FFFFFF),
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = c }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Acciones Cancelar / Guardar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { onIntent(EditorIntent.CloseTextEditor) }) {
                        Text("Cancelar", color = TextMuted)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            val props = initialProperties.copy(
                                text = text.ifBlank { "Texto" },
                                fontSize = fontSize,
                                textColor = selectedColor
                            )
                            onIntent(EditorIntent.AddTextLayer(props))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StudioAccent)
                    ) {
                        Text("Añadir")
                    }
                }
            }
        }
    }
}
