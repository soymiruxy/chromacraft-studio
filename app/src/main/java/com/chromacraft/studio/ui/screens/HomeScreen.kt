package com.chromacraft.studio.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chromacraft.studio.core.project.ProjectHeader
import com.chromacraft.studio.ui.theme.StudioAccent
import com.chromacraft.studio.ui.theme.StudioDark
import com.chromacraft.studio.ui.theme.StudioSurface
import com.chromacraft.studio.ui.theme.StudioSurfaceVariant
import com.chromacraft.studio.ui.theme.TextMuted
import com.chromacraft.studio.ui.theme.TextPrimary
import com.chromacraft.studio.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Pantalla de inicio de ChromaCraft Studio.
 * Diseñada bajo Material You: punto de entrada con acceso a creación rápida,
 * selector de fotos de galería y galería de proyectos recientes con miniaturas visuales.
 */
@Composable
fun HomeScreen(
    recentProjects: List<ProjectHeader>,
    recoveryAvailable: Boolean,
    onOpenGallery: () -> Unit,
    onCreateNewCanvas: (name: String, width: Int, height: Int, transparentBg: Boolean, bgColor: Long) -> Unit,
    onOpenProject: (projectId: String) -> Unit,
    onDeleteProject: (projectId: String) -> Unit,
    onRecoverSession: () -> Unit,
    onDiscardRecovery: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showNewCanvasDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Cabecera de la Pantalla de Inicio
        Surface(
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth(),
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(StudioAccent),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("CC", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "ChromaCraft Studio",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Estudio Creativo Profesional",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                }

                Button(
                    onClick = { showNewCanvasDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = StudioAccent),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Nuevo", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Crear", fontSize = 13.sp)
                }
            }
        }

        // Banner de Recuperación de Sesión previa si ocurrió un cierre repentino
        if (recoveryAvailable) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = StudioSurfaceVariant)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.History, contentDescription = "Recuperación", tint = StudioAccent)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Sesión no guardada detectada", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            Text("¿Deseas restaurar el trabajo previo?", fontSize = 11.sp, color = TextSecondary)
                        }
                    }
                    Row {
                        IconButton(onClick = onDiscardRecovery) {
                            Text("✖", color = TextMuted)
                        }
                        Button(
                            onClick = onRecoverSession,
                            colors = ButtonDefaults.buttonColors(containerColor = StudioAccent),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Restaurar", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Acciones Rápidas (Importar Foto vs. Nuevo Lienzo)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Tarjeta 1: Importar Foto
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .clickable(onClick = onOpenGallery),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = StudioSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(StudioAccent.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Galería", tint = StudioAccent)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Importar Foto", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Text("Editar imagen existente", fontSize = 11.sp, color = TextSecondary)
                }
            }

            // Tarjeta 2: Lienzo en Blanco
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { showNewCanvasDialog = true },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = StudioSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFFF4081).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "Lienzo", tint = Color(0xFFFF4081))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Lienzo en Blanco", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Text("Empezar desde cero", fontSize = 11.sp, color = TextSecondary)
                }
            }
        }

        // Sección: Proyectos Recientes
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Proyectos Recientes", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Text("${recentProjects.size} proyectos", fontSize = 12.sp, color = TextMuted)
        }

        if (recentProjects.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = "Sin proyectos",
                        modifier = Modifier.size(56.dp),
                        tint = TextMuted.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Aún no tienes proyectos guardados", fontSize = 14.sp, color = TextSecondary)
                    Text("Toca 'Importar Foto' o 'Crear' para comenzar", fontSize = 12.sp, color = TextMuted)
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(recentProjects, key = { it.id }) { project ->
                    ProjectCard(
                        project = project,
                        onOpen = { onOpenProject(project.id) },
                        onDelete = { onDeleteProject(project.id) }
                    )
                }
            }
        }
    }

    if (showNewCanvasDialog) {
        NewCanvasDialog(
            onDismiss = { showNewCanvasDialog = false },
            onCreate = { name, w, h, transparent, bg ->
                showNewCanvasDialog = false
                onCreateNewCanvas(name, w, h, transparent, bg)
            }
        )
    }
}

@Composable
private fun ProjectCard(
    project: ProjectHeader,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    val dateStr = remember(project.modifiedTimestamp) {
        val sdf = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
        sdf.format(Date(project.modifiedTimestamp))
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = StudioSurface),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onOpen)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                if (project.thumbnailBitmap != null && !project.thumbnailBitmap.isRecycled) {
                    Image(
                        bitmap = project.thumbnailBitmap.asImageBitmap(),
                        contentDescription = project.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text("CC", color = TextMuted, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }

                // Chip de resolución sobre la miniatura
                Surface(
                    color = Color.Black.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                ) {
                    Text(
                        text = "${project.width}×${project.height}",
                        color = Color.White,
                        fontSize = 9.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = project.name,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        maxLines = 1
                    )
                    Text(
                        text = dateStr,
                        fontSize = 10.sp,
                        color = TextMuted
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Eliminar",
                        tint = Color(0xFFFF5252).copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
