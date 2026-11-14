package com.chromacraft.studio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chromacraft.studio.core.model.ToolType
import com.chromacraft.studio.ui.theme.StudioAccent
import com.chromacraft.studio.ui.theme.StudioDark
import com.chromacraft.studio.ui.theme.StudioSurface
import com.chromacraft.studio.ui.theme.TextMuted
import com.chromacraft.studio.ui.theme.TextPrimary
import com.chromacraft.studio.viewmodel.EditorIntent

data class ToolItemData(
    val type: ToolType,
    val icon: ImageVector,
    val label: String
)

val PrimaryTools = listOf(
    ToolItemData(ToolType.CROP, Icons.Default.Crop, "Recorte"),
    ToolItemData(ToolType.ADJUST, Icons.Default.Tune, "Ajustes"),
    ToolItemData(ToolType.CURVES, Icons.Default.ShowChart, "Curvas"),
    ToolItemData(ToolType.FILTER, Icons.Default.AutoAwesome, "Filtros"),
    ToolItemData(ToolType.BLUR_SHARPEN, Icons.Default.BlurOn, "Óptica"),
    ToolItemData(ToolType.CUTOUT, Icons.Default.ContentCut, "Recorte AI"),
    ToolItemData(ToolType.DRAW, Icons.Default.Brush, "Dibujo"),
    ToolItemData(ToolType.TEXT, Icons.Default.TextFields, "Texto"),
    ToolItemData(ToolType.STICKER, Icons.Default.Category, "Stickers"),
    ToolItemData(ToolType.LAYERS, Icons.Default.Layers, "Capas")
)

/**
 * Barra principal de herramientas inferior (modo teléfono).
 */
@Composable
fun BottomToolBar(
    activeTool: ToolType,
    onIntent: (EditorIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(74.dp),
        color = StudioDark
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PrimaryTools.forEach { tool ->
                val isSelected = activeTool == tool.type
                ToolButton(
                    item = tool,
                    isSelected = isSelected,
                    onClick = {
                        if (isSelected) {
                            onIntent(EditorIntent.CloseTool)
                        } else {
                            if (tool.type == ToolType.TEXT) {
                                onIntent(EditorIntent.OpenTextEditor())
                            } else {
                                onIntent(EditorIntent.SelectTool(tool.type))
                            }
                        }
                    }
                )
            }
        }
    }
}

/**
 * Riel de navegación lateral vertical (modo tableta y horizontal).
 */
@Composable
fun SideToolRail(
    activeTool: ToolType,
    onIntent: (EditorIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .width(84.dp)
            .fillMaxHeight(),
        color = StudioDark
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 12.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PrimaryTools.forEach { tool ->
                val isSelected = activeTool == tool.type
                ToolButton(
                    item = tool,
                    isSelected = isSelected,
                    onClick = {
                        if (isSelected) {
                            onIntent(EditorIntent.CloseTool)
                        } else {
                            if (tool.type == ToolType.TEXT) {
                                onIntent(EditorIntent.OpenTextEditor())
                            } else {
                                onIntent(EditorIntent.SelectTool(tool.type))
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun ToolButton(
    item: ToolItemData,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (isSelected) StudioSurface else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.label,
                tint = if (isSelected) StudioAccent else TextPrimary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = item.label,
                fontSize = 10.sp,
                color = if (isSelected) StudioAccent else TextMuted
            )
        }
    }
}
