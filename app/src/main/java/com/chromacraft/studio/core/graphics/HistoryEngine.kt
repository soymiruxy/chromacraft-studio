package com.chromacraft.studio.core.graphics

import com.chromacraft.studio.core.model.Layer

/**
 * Motor de historial no destructivo para la pila Deshacer / Rehacer (Undo / Redo).
 * Emplea copias inmutables de la estructura de capas para evitar consumo excesivo de RAM,
 * limitando la profundidad de la pila a 25 estados con desalojo FIFO.
 */
class HistoryEngine(private val maxDepth: Int = 25) {

    data class HistorySnapshot(
        val layers: List<Layer>,
        val selectedLayerId: String?,
        val description: String
    )

    private val undoStack = ArrayDeque<HistorySnapshot>()
    private val redoStack = ArrayDeque<HistorySnapshot>()

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    /**
     * Registra un nuevo estado en la pila de historial y purga la pila de rehacer.
     */
    fun pushSnapshot(snapshot: HistorySnapshot) {
        if (undoStack.size >= maxDepth) {
            undoStack.removeFirst() // Desalojar el estado más antiguo
        }
        undoStack.addLast(snapshot)
        redoStack.clear()
    }

    /**
     * Retrocede al estado anterior, devolviendo el estado recuperado
     * y almacenando el estado actual en la pila de rehacer.
     */
    fun undo(currentSnapshot: HistorySnapshot): HistorySnapshot? {
        if (!canUndo) return null
        val previous = undoStack.removeLast()
        redoStack.addLast(currentSnapshot)
        return previous
    }

    /**
     * Avanza al estado siguiente deshecho previamente.
     */
    fun redo(currentSnapshot: HistorySnapshot): HistorySnapshot? {
        if (!canRedo) return null
        val next = redoStack.removeLast()
        undoStack.addLast(currentSnapshot)
        return next
    }

    fun clear() {
        undoStack.clear()
        redoStack.clear()
    }
}
