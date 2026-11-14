package com.chromacraft.studio.ui.components

import android.graphics.Color as AndroidColor
import android.graphics.DashPathEffect
import android.graphics.Paint as AndroidPaint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.chromacraft.studio.core.graphics.CanvasEngine
import com.chromacraft.studio.core.graphics.TransformEngine
import com.chromacraft.studio.core.model.StrokePath
import com.chromacraft.studio.core.model.ToolType
import com.chromacraft.studio.ui.theme.StudioBlack
import com.chromacraft.studio.viewmodel.EditorIntent
import com.chromacraft.studio.viewmodel.EditorUiState
import kotlin.math.atan2
import kotlin.math.hypot

/**
 * Lienzo principal interactivo con soporte para:
 * - Ajuste dinámico responsivo en pantalla sin deformación de aspecto.
 * - Zoom & Pan libre con doble toque para restablecer.
 * - Manipulación táctil suave y directa 1:1 de capas (Gizmo de Picsart).
 * - Dibujo con sensibilidad a la presión del stylus e interpolación Bézier.
 * - Herramienta de selección por lazo (Cutout).
 */
@Composable
fun EditorCanvas(
    uiState: EditorUiState,
    onIntent: (EditorIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val currentStrokePoints = remember { mutableStateListOf<Offset>() }
    val currentStrokePressures = remember { mutableStateListOf<Float>() }
    var activeGizmoHandle by remember { mutableStateOf(TransformEngine.GizmoHandle.NONE) }

    // Estado del viewport: Zoom y Pan
    var viewportScale by remember { mutableFloatStateOf(1.0f) }
    var viewportPan by remember { mutableStateOf(Offset.Zero) }

    // Variables de referencia para el cálculo de escala y rotación del Gizmo
    var initialDistToCenter by remember { mutableFloatStateOf(1f) }
    var initialAngleToCenter by remember { mutableFloatStateOf(0f) }
    var initialLayerScale by remember { mutableFloatStateOf(1f) }
    var initialLayerRotation by remember { mutableFloatStateOf(0f) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(StudioBlack),
        contentAlignment = Alignment.Center
    ) {
        val containerWidth = constraints.maxWidth.toFloat()
        val containerHeight = constraints.maxHeight.toFloat()

        // Dimensiones del lienzo preservando relación de aspecto exacta del proyecto
        val safeProjW = uiState.canvasWidth.toFloat().coerceAtLeast(100f)
        val safeProjH = uiState.canvasHeight.toFloat().coerceAtLeast(100f)
        val canvasAspect = safeProjW / safeProjH
        val containerAspect = containerWidth / containerHeight.coerceAtLeast(1f)

        val paddingPx = with(density) { 24.dp.toPx() }
        val availableW = (containerWidth - paddingPx).coerceAtLeast(100f)
        val availableH = (containerHeight - paddingPx).coerceAtLeast(100f)

        val (fittedWidth, fittedHeight) = if (canvasAspect > containerAspect) {
            val w = availableW
            val h = w / canvasAspect
            Pair(w, h)
        } else {
            val h = availableH
            val w = h * canvasAspect
            Pair(w, h)
        }

        val canvasWidthDp = with(density) { fittedWidth.toDp() }
        val canvasHeightDp = with(density) { fittedHeight.toDp() }

        Box(
            modifier = Modifier
                .size(canvasWidthDp, canvasHeightDp)
                .graphicsLayer(
                    scaleX = viewportScale,
                    scaleY = viewportScale,
                    translationX = viewportPan.x,
                    translationY = viewportPan.y
                )
                // Doble toque para restablecer zoom y paneo
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            viewportScale = 1.0f
                            viewportPan = Offset.Zero
                        }
                    )
                }
                .pointerInput(uiState.activeTool, uiState.selectedLayerId, fittedWidth, fittedHeight) {
                    when (uiState.activeTool) {
                        ToolType.DRAW -> {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    currentStrokePoints.clear()
                                    currentStrokePressures.clear()
                                    currentStrokePoints.add(offset)
                                    currentStrokePressures.add(1.0f)
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    currentStrokePoints.add(change.position)
                                    currentStrokePressures.add(change.pressure.coerceIn(0.1f, 1.0f))
                                },
                                onDragEnd = {
                                    if (currentStrokePoints.isNotEmpty()) {
                                        val stroke = StrokePath(
                                            points = currentStrokePoints.toList(),
                                            pressures = currentStrokePressures.toList(),
                                            brushType = uiState.brushProperties.brushType,
                                            color = uiState.brushProperties.color,
                                            strokeWidth = uiState.brushProperties.size,
                                            opacity = uiState.brushProperties.opacity,
                                            isEraser = uiState.brushProperties.isEraser
                                        )
                                        onIntent(EditorIntent.AddStroke(stroke))
                                        currentStrokePoints.clear()
                                        currentStrokePressures.clear()
                                    }
                                },
                                onDragCancel = {
                                    currentStrokePoints.clear()
                                    currentStrokePressures.clear()
                                }
                            )
                        }

                        ToolType.CUTOUT -> {
                            // Captura de selección por lazo
                            detectDragGestures(
                                onDragStart = { offset ->
                                    onIntent(EditorIntent.ClearLasso)
                                    onIntent(EditorIntent.AddLassoPoint(offset))
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    onIntent(EditorIntent.AddLassoPoint(change.position))
                                }
                            )
                        }

                        else -> {
                            // Manipulación libre de capas mediante Gizmo (Picsart style)
                            detectDragGestures(
                                onDragStart = { touchOffset ->
                                    val selected = uiState.selectedLayer
                                    if (selected != null && !selected.isLocked) {
                                        val corners = TransformEngine.getTransformedCorners(
                                            width = fittedWidth,
                                            height = fittedHeight,
                                            translationX = selected.translationX,
                                            translationY = selected.translationY,
                                            scaleX = selected.scaleX,
                                            scaleY = selected.scaleY,
                                            rotationDegrees = selected.rotationDegrees
                                        )

                                        activeGizmoHandle = TransformEngine.hitTestGizmo(
                                            touchX = touchOffset.x,
                                            touchY = touchOffset.y,
                                            corners = corners,
                                            handleRadiusPx = with(density) { 36.dp.toPx() }
                                        )

                                        when (activeGizmoHandle) {
                                            TransformEngine.GizmoHandle.DELETE -> {
                                                onIntent(EditorIntent.DeleteLayer(selected.id))
                                                activeGizmoHandle = TransformEngine.GizmoHandle.NONE
                                            }

                                            TransformEngine.GizmoHandle.DUPLICATE -> {
                                                onIntent(EditorIntent.DuplicateLayer(selected.id))
                                                activeGizmoHandle = TransformEngine.GizmoHandle.NONE
                                            }

                                            TransformEngine.GizmoHandle.LOCK_TOGGLE -> {
                                                onIntent(EditorIntent.LockLayer(selected.id, !selected.isLocked))
                                                activeGizmoHandle = TransformEngine.GizmoHandle.NONE
                                            }

                                            TransformEngine.GizmoHandle.SCALE_ROTATE -> {
                                                val layerCenterX = fittedWidth / 2f + selected.translationX
                                                val layerCenterY = fittedHeight / 2f + selected.translationY
                                                val dx = touchOffset.x - layerCenterX
                                                val dy = touchOffset.y - layerCenterY
                                                initialDistToCenter = hypot(dx, dy).coerceAtLeast(1f)
                                                initialAngleToCenter = Math.toDegrees(atan2(dy, dx).toDouble()).toFloat()
                                                initialLayerScale = selected.scaleX
                                                initialLayerRotation = selected.rotationDegrees
                                            }

                                            else -> Unit
                                        }
                                    } else {
                                        activeGizmoHandle = TransformEngine.GizmoHandle.NONE
                                    }
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val selected = uiState.selectedLayer ?: return@detectDragGestures

                                    when (activeGizmoHandle) {
                                        TransformEngine.GizmoHandle.BODY -> {
                                            // Desplazamiento suave directo 1:1 bajo el dedo
                                            val dx = dragAmount.x / viewportScale
                                            val dy = dragAmount.y / viewportScale
                                            onIntent(
                                                EditorIntent.UpdateLayerTransform(
                                                    layerId = selected.id,
                                                    translationX = selected.translationX + dx,
                                                    translationY = selected.translationY + dy,
                                                    scaleX = selected.scaleX,
                                                    scaleY = selected.scaleY,
                                                    rotationDegrees = selected.rotationDegrees
                                                )
                                            )
                                        }

                                        TransformEngine.GizmoHandle.SCALE_ROTATE -> {
                                            val layerCenterX = fittedWidth / 2f + selected.translationX
                                            val layerCenterY = fittedHeight / 2f + selected.translationY
                                            val (factor, deltaAngle) = TransformEngine.computeScaleAndRotation(
                                                centerX = layerCenterX,
                                                centerY = layerCenterY,
                                                touchX = change.position.x,
                                                touchY = change.position.y,
                                                initialDistance = initialDistToCenter,
                                                initialAngleDegrees = initialAngleToCenter
                                            )

                                            val newScale = (initialLayerScale * factor).coerceIn(0.1f, 8f)
                                            val newRotation = initialLayerRotation + deltaAngle

                                            onIntent(
                                                EditorIntent.UpdateLayerTransform(
                                                    layerId = selected.id,
                                                    translationX = selected.translationX,
                                                    translationY = selected.translationY,
                                                    scaleX = newScale,
                                                    scaleY = newScale,
                                                    rotationDegrees = newRotation
                                                )
                                            )
                                        }

                                        else -> Unit
                                    }
                                },
                                onDragEnd = { activeGizmoHandle = TransformEngine.GizmoHandle.NONE },
                                onDragCancel = { activeGizmoHandle = TransformEngine.GizmoHandle.NONE }
                            )
                        }
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawIntoCanvas { canvas ->
                    val nativeCanvas = canvas.nativeCanvas

                    // Fondo ajedrezado si la transparencia está activa
                    if (uiState.transparentBackground) {
                        drawCheckerboard(nativeCanvas, size.width, size.height)
                    }

                    // 1. Renderizar capas con aceleración de hardware
                    CanvasEngine.renderLayers(
                        canvas = nativeCanvas,
                        layers = uiState.layers,
                        targetWidth = size.width.toInt(),
                        targetHeight = size.height.toInt(),
                        drawBackground = !uiState.transparentBackground
                    )

                    // 2. Trazo en curso en tiempo real
                    if (uiState.activeTool == ToolType.DRAW && currentStrokePoints.size > 1) {
                        val tempStroke = StrokePath(
                            points = currentStrokePoints.toList(),
                            pressures = currentStrokePressures.toList(),
                            brushType = uiState.brushProperties.brushType,
                            color = uiState.brushProperties.color,
                            strokeWidth = uiState.brushProperties.size,
                            opacity = uiState.brushProperties.opacity,
                            isEraser = uiState.brushProperties.isEraser
                        )
                        val p = com.chromacraft.studio.core.graphics.BrushEngine.createPaintForStroke(tempStroke)
                        val path = com.chromacraft.studio.core.graphics.BrushEngine.createSmoothPath(currentStrokePoints.toList())
                        nativeCanvas.drawPath(path, p)
                    }

                    // 3. Trazado de selección por lazo
                    if (uiState.activeTool == ToolType.CUTOUT && uiState.lassoPoints.size > 1) {
                        drawLassoPath(nativeCanvas, uiState.lassoPoints)
                    }

                    // 4. Gizmo interactivo de Picsart
                    val selected = uiState.selectedLayer
                    if (selected != null && uiState.activeTool != ToolType.DRAW && uiState.activeTool != ToolType.CROP && uiState.activeTool != ToolType.CUTOUT) {
                        drawGizmo(
                            nativeCanvas = nativeCanvas,
                            width = size.width,
                            height = size.height,
                            layer = selected
                        )
                    }
                }
            }
        }
    }
}

private fun drawCheckerboard(nativeCanvas: android.graphics.Canvas, width: Float, height: Float) {
    val checkSize = 32f
    val p1 = AndroidPaint().apply { color = 0xFF1E212B.toInt() }
    val p2 = AndroidPaint().apply { color = 0xFF14161D.toInt() }

    var y = 0f
    var row = 0
    while (y < height) {
        var x = 0f
        var col = 0
        while (x < width) {
            val paint = if ((row + col) % 2 == 0) p1 else p2
            nativeCanvas.drawRect(x, y, x + checkSize, y + checkSize, paint)
            x += checkSize
            col++
        }
        y += checkSize
        row++
    }
}

private fun drawLassoPath(nativeCanvas: android.graphics.Canvas, points: List<Offset>) {
    val path = android.graphics.Path().apply {
        moveTo(points[0].x, points[0].y)
        for (i in 1 until points.size) {
            lineTo(points[i].x, points[i].y)
        }
    }

    val lassoPaint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF00E5FF.toInt()
        style = AndroidPaint.Style.STROKE
        strokeWidth = 3f
        pathEffect = DashPathEffect(floatArrayOf(10f, 6f), 0f)
    }
    nativeCanvas.drawPath(path, lassoPaint)
}

private fun drawGizmo(
    nativeCanvas: android.graphics.Canvas,
    width: Float,
    height: Float,
    layer: com.chromacraft.studio.core.model.Layer
) {
    val corners = TransformEngine.getTransformedCorners(
        width = width,
        height = height,
        translationX = layer.translationX,
        translationY = layer.translationY,
        scaleX = layer.scaleX,
        scaleY = layer.scaleY,
        rotationDegrees = layer.rotationDegrees
    )

    val borderPaint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF7C4DFF.toInt()
        style = AndroidPaint.Style.STROKE
        strokeWidth = 3f
        pathEffect = DashPathEffect(floatArrayOf(12f, 8f), 0f)
    }

    val polyPath = android.graphics.Path().apply {
        moveTo(corners[0], corners[1])
        lineTo(corners[2], corners[3])
        lineTo(corners[4], corners[5])
        lineTo(corners[6], corners[7])
        close()
    }
    nativeCanvas.drawPath(polyPath, borderPaint)

    val handleBgPaint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.WHITE
        style = AndroidPaint.Style.FILL
        setShadowLayer(6f, 0f, 2f, 0x66000000)
    }

    val handleAccentPaint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF7C4DFF.toInt()
        style = AndroidPaint.Style.FILL
    }

    val deletePaint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFF5252.toInt()
        style = AndroidPaint.Style.FILL
    }

    val radius = 24f

    // 1. Esquina Superior Izquierda: Eliminar capa
    nativeCanvas.drawCircle(corners[0], corners[1], radius, deletePaint)
    val xPaint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.WHITE
        strokeWidth = 3.5f
        style = AndroidPaint.Style.STROKE
    }
    val d = 8f
    nativeCanvas.drawLine(corners[0] - d, corners[1] - d, corners[0] + d, corners[1] + d, xPaint)
    nativeCanvas.drawLine(corners[0] + d, corners[1] - d, corners[0] - d, corners[1] + d, xPaint)

    // 2. Esquina Superior Derecha: Duplicar capa
    nativeCanvas.drawCircle(corners[2], corners[3], radius, handleBgPaint)
    val dupPaint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF7C4DFF.toInt()
        strokeWidth = 2.5f
        style = AndroidPaint.Style.STROKE
    }
    nativeCanvas.drawRect(corners[2] - 7f, corners[3] - 7f, corners[2] + 4f, corners[3] + 4f, dupPaint)
    nativeCanvas.drawRect(corners[2] - 4f, corners[3] - 4f, corners[2] + 7f, corners[3] + 7f, dupPaint)

    // 3. Esquina Inferior Derecha: Escalar / Rotar
    nativeCanvas.drawCircle(corners[4], corners[5], radius, handleAccentPaint)
    val rotateArrowPaint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.WHITE
        strokeWidth = 2.5f
        style = AndroidPaint.Style.STROKE
    }
    nativeCanvas.drawCircle(corners[4], corners[5], 10f, rotateArrowPaint)

    // 4. Esquina Inferior Izquierda: Bloqueo / Desbloqueo
    nativeCanvas.drawCircle(corners[6], corners[7], radius * 0.85f, handleBgPaint)
    val lockPaint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF7C4DFF.toInt()
        strokeWidth = 2f
        style = AndroidPaint.Style.STROKE
    }
    nativeCanvas.drawCircle(corners[6], corners[7] - 3f, 5f, lockPaint)
    nativeCanvas.drawRect(corners[6] - 6f, corners[7] - 2f, corners[6] + 6f, corners[7] + 6f, lockPaint)
}
