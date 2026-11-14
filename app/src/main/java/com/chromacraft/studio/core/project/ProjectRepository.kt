package com.chromacraft.studio.core.project

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.chromacraft.studio.core.graphics.BitmapUtils
import com.chromacraft.studio.core.graphics.CanvasEngine
import com.chromacraft.studio.core.model.Layer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

/**
 * Metadatos ligeros de cabecera de proyecto para visualización rápida en la pantalla de inicio.
 */
data class ProjectHeader(
    val id: String,
    val name: String,
    val width: Int,
    val height: Int,
    val modifiedTimestamp: Long,
    val thumbnailBitmap: Bitmap?,
    val file: File
)

/**
 * Repositorio central de persistencia y gestión de proyectos editables (.chroma).
 */
class ProjectRepository(private val context: Context) {

    private val projectsDir: File
        get() = File(context.filesDir, "projects").apply { if (!exists()) mkdirs() }

    private val thumbsDir: File
        get() = File(context.filesDir, "thumbnails").apply { if (!exists()) mkdirs() }

    /**
     * Lista todos los proyectos guardados ordenados cronológicamente por última modificación.
     */
    suspend fun listProjects(): List<ProjectHeader> = withContext(Dispatchers.IO) {
        val files = projectsDir.listFiles { f -> f.extension == "chroma" } ?: return@withContext emptyList()
        val headers = mutableListOf<ProjectHeader>()

        for (f in files) {
            try {
                val jsonString = f.readText()
                val json = JSONObject(jsonString)
                val id = f.nameWithoutExtension
                val name = json.optString("name", "Proyecto sin título")
                val width = json.optInt("width", 1080)
                val height = json.optInt("height", 1080)

                // Cargar miniatura si existe
                val thumbFile = File(thumbsDir, "$id.thumb")
                val thumbBmp = if (thumbFile.exists()) {
                    BitmapFactory.decodeFile(thumbFile.absolutePath)
                } else null

                headers.add(
                    ProjectHeader(
                        id = id,
                        name = name,
                        width = width,
                        height = height,
                        modifiedTimestamp = f.lastModified(),
                        thumbnailBitmap = thumbBmp,
                        file = f
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        headers.sortedByDescending { it.modifiedTimestamp }
    }

    /**
     * Guarda el proyecto y genera su miniatura visual en segundo plano.
     */
    suspend fun saveProject(
        projectId: String,
        name: String,
        width: Int,
        height: Int,
        transparentBg: Boolean,
        layers: List<Layer>
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val json = ProjectSerializer.serializeProject(name, width, height, transparentBg, layers)
            val projectFile = File(projectsDir, "$projectId.chroma")
            projectFile.writeText(json)

            // Generar miniatura
            val previewBmp = CanvasEngine.renderFullResolution(
                layers = layers,
                width = minOf(width, 512),
                height = minOf(height, 512),
                keepTransparentBackground = transparentBg
            )
            val thumbFile = File(thumbsDir, "$projectId.thumb")
            FileOutputStream(thumbFile).use { fos ->
                previewBmp.compress(Bitmap.CompressFormat.JPEG, 85, fos)
            }
            previewBmp.recycle()

            Result.success(projectFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Elimina el archivo de proyecto y su miniatura asociada.
     */
    suspend fun deleteProject(projectId: String): Boolean = withContext(Dispatchers.IO) {
        val pFile = File(projectsDir, "$projectId.chroma")
        val tFile = File(thumbsDir, "$projectId.thumb")
        val pDeleted = if (pFile.exists()) pFile.delete() else true
        val tDeleted = if (tFile.exists()) tFile.delete() else true
        pDeleted && tDeleted
    }
}
