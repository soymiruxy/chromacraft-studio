package com.chromacraft.studio.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chromacraft.studio.ui.theme.StudioAccent
import com.chromacraft.studio.ui.theme.StudioDark
import com.chromacraft.studio.ui.theme.StudioSurface
import com.chromacraft.studio.ui.theme.TextMuted
import com.chromacraft.studio.ui.theme.TextPrimary
import com.chromacraft.studio.viewmodel.EditorIntent
import com.chromacraft.studio.viewmodel.EditorUiState

/**
 * Barra superior global de utilidades con soporte para:
 * - Regreso a Pantalla de Inicio.
 * - Comparador en tiempo real "Antes / Después" (mantener presionado).
 * - Historial Deshacer/Rehacer.
 * - Información de dimensiones.
 * - Exportación.
 */
@Composable
fun TopBar(
    uiState: EditorUiState,
    onIntent: (EditorIntent) -> Unit,
    onNavigateHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        color = StudioDark
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Botón Salir / Regresar a Inicio
            IconButton(onClick = onNavigateHome) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Volver al Inicio",
                    tint = TextPrimary
                )
            }

            // Chip interactivo de resolución del lienzo (Abre ResizeDialog)
            Surface(
                color = StudioSurface,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onIntent(EditorIntent.OpenResizeDialog) }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AspectRatio,
                        contentDescription = "Dimensiones",
                        tint = StudioAccent,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${uiState.canvasWidth} × ${uiState.canvasHeight}",
                        color = TextPrimary,
                        fontSize = 11.sp
                    )
                }
            }

            // Acciones: Comparador Antes/Después, Deshacer, Rehacer, Exportar
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Botón Comparar Antes/Después (mantener presionado para ver original)
                Surface(
                    color = if (uiState.isComparingOriginal) StudioAccent else StudioSurface,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    onIntent(EditorIntent.SetComparingOriginal(true))
                                    tryAwaitRelease()
                                    onIntent(EditorIntent.SetComparingOriginal(false))
                                }
                            )
                        }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Compare,
                            contentDescription = "Comparar Antes / Después",
                            tint = if (uiState.isComparingOriginal) Color.White else TextPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (uiState.isComparingOriginal) "Original" else "Comparar",
                            fontSize = 10.sp,
                            color = if (uiState.isComparingOriginal) Color.White else TextPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                IconButton(
                    onClick = { onIntent(EditorIntent.Undo) },
                    enabled = uiState.canUndo
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Undo,
                        contentDescription = "Deshacer",
                        tint = if (uiState.canUndo) TextPrimary else TextMuted
                    )
                }

                IconButton(
                    onClick = { onIntent(EditorIntent.Redo) },
                    enabled = uiState.canRedo
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Redo,
                        contentDescription = "Rehacer",
                        tint = if (uiState.canRedo) TextPrimary else TextMuted
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Botón Exportar
                Button(
                    onClick = { onIntent(EditorIntent.OpenExportDialog) },
                    enabled = !uiState.isExporting,
                    colors = ButtonDefaults.buttonColors(containerColor = StudioAccent),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = ButtonDefaults.ButtonWithIconContentPadding
                ) {
                    if (uiState.isExporting) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Guardar",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Guardar", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
