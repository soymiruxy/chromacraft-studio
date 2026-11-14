package com.chromacraft.studio.core.graphics

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.chromacraft.studio.core.model.Layer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

/**
 * Motor de exportación a alta resolución e integración con el sistema Scoped Storage de Android.
 * Soporta PNG (con transparencia), JPEG (con control de calidad 1-100%) y WebP.
 */
object ExportEngine {

    enum class ExportFormat(val displayName: String, val mimeType: String, val extension: String, val supportsTransparency: Boolean) {
        PNG("PNG (Sin pérdida / Transparente)", "image/png", ".png", true),
        JPEG("JPEG (Comprimido)", "image/jpeg", ".jpg", false),
        WEBP("WebP (Eficiente)", "image/webp", ".webp", true)
    }

    enum class ResolutionScale(val label: String, val scale: Float) {
        HALF("Media Resolución (0.5×)", 0.5f),
        ORIGINAL("Resolución Estándar (1.0×)", 1.0f),
        ULTRA_HD("Ultra HD (2.0×)", 2.0f)
    }

    /**
     * Renderiza y guarda la composición en la galería pública mediante MediaStore.
     */
    suspend fun exportToGallery(
        context: Context,
        layers: List<Layer>,
        baseWidth: Int,
        baseHeight: Int,
        format: ExportFormat = ExportFormat.PNG,
        scale: ResolutionScale = ResolutionScale.ORIGINAL,
        quality: Int = 95,
        transparentBackground: Boolean = false
    ): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val targetW = (baseWidth * scale.scale).toInt()
            val targetH = (baseHeight * scale.scale).toInt()

            val shouldBeTransparent = transparentBackground && format.supportsTransparency
            val bitmap = CanvasEngine.renderFullResolution(
                layers = layers,
                width = targetW,
                height = targetH,
                keepTransparentBackground = shouldBeTransparent
            )

            val filename = "ChromaCraft_${System.currentTimeMillis()}${format.extension}"

            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, format.mimeType)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/ChromaCraft")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                ?: return@withContext Result.failure(Exception("No se pudo registrar la entrada en MediaStore"))

            resolver.openOutputStream(uri)?.use { stream ->
                compressBitmap(bitmap, format, quality, stream)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
            }

            bitmap.recycle()
            Result.success(uri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Prepara un archivo temporal en cache para compartir inmediatamente en redes sociales.
     */
    suspend fun createShareIntent(
        context: Context,
        layers: List<Layer>,
        width: Int,
        height: Int,
        format: ExportFormat = ExportFormat.PNG
    ): Result<Intent> = withContext(Dispatchers.IO) {
        try {
            val bitmap = CanvasEngine.renderFullResolution(layers, width, height, keepTransparentBackground = false)
            val cacheFile = File(context.cacheDir, "share_image_${System.currentTimeMillis()}${format.extension}")
            FileOutputStream(cacheFile).use { fos ->
                compressBitmap(bitmap, format, 95, fos)
            }
            bitmap.recycle()

            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, cacheFile)

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = format.mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            Result.success(Intent.createChooser(intent, "Compartir composición"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun compressBitmap(
        bitmap: Bitmap,
        format: ExportFormat,
        quality: Int,
        stream: OutputStream
    ) {
        val q = quality.coerceIn(1, 100)
        when (format) {
            ExportFormat.PNG -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            ExportFormat.JPEG -> bitmap.compress(Bitmap.CompressFormat.JPEG, q, stream)
            ExportFormat.WEBP -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    if (q == 100) {
                        bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSLESS, 100, stream)
                    } else {
                        bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, q, stream)
                    }
                } else {
                    @Suppress("DEPRECATION")
                    bitmap.compress(Bitmap.CompressFormat.WEBP, q, stream)
                }
            }
        }
    }
}
