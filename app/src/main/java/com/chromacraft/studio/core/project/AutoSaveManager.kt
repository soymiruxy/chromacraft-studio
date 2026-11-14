package com.chromacraft.studio.core.project

import android.content.Context
import com.chromacraft.studio.core.model.Layer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Gestor de guardado automático no bloqueante con temporizador debounced en segundo plano.
 * Previene pérdida de trabajo ante cierres accidentales o reinicios del proceso.
 */
class AutoSaveManager(
    private val context: Context,
    private val scope: CoroutineScope
) {

    private val autoSaveFile: File
        get() = File(context.filesDir, "autosave_session.chroma")

    private var autoSaveJob: Job? = null

    /**
     * Programa un guardado automático tras 1500ms de inactividad táctil.
     */
    fun scheduleAutoSave(
        projectName: String,
        width: Int,
        height: Int,
        transparentBg: Boolean,
        layers: List<Layer>
    ) {
        autoSaveJob?.cancel()
        autoSaveJob = scope.launch(Dispatchers.IO) {
            delay(1500) // Debounce de 1.5 segundos
            try {
                val json = ProjectSerializer.serializeProject(
                    projectName = projectName,
                    width = width,
                    height = height,
                    transparentBg = transparentBg,
                    layers = layers
                )
                autoSaveFile.writeText(json)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Verifica si existe una sesión previa interrumpida para recuperación automática.
     */
    suspend fun hasRecoverySession(): Boolean = withContext(Dispatchers.IO) {
        autoSaveFile.exists() && autoSaveFile.length() > 50
    }

    suspend fun readRecoverySession(): String? = withContext(Dispatchers.IO) {
        if (hasRecoverySession()) autoSaveFile.readText() else null
    }

    suspend fun clearRecoverySession() = withContext(Dispatchers.IO) {
        if (autoSaveFile.exists()) autoSaveFile.delete()
    }
}
