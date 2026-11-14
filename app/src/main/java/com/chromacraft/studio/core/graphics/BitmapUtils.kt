package com.chromacraft.studio.core.graphics

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import kotlin.math.max
import kotlin.math.min

/**
 * Utilidades de alto rendimiento para decodificación, escalado seguro y gestión de memoria de mapas de bits.
 * Ejecuta todas las operaciones intensivas estrictamente fuera del hilo principal de UI.
 */
object BitmapUtils {

    /**
     * Límite de resolución máxima para el viewport interactivo en tiempo real (evita colapsos OOM).
     */
    const val MAX_PROXY_DIMENSION = 2048

    /**
     * Decodifica de forma segura un mapa de bits desde un Uri aplicando submuestreo progresivo (inSampleSize)
     * y corrigiendo la orientación EXIF de la cámara automáticamente.
     */
    suspend fun decodeSampledBitmapFromUri(
        context: Context,
        uri: Uri,
        reqWidth: Int = MAX_PROXY_DIMENSION,
        reqHeight: Int = MAX_PROXY_DIMENSION
    ): Result<Bitmap> = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver

            // 1. Decodificar solo límites (inJustDecodeBounds) para no asignar memoria
            val boundsStream = contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(Exception("No se pudo abrir el flujo de datos de la imagen"))

            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(boundsStream, null, options)
            boundsStream.close()

            // 2. Calcular factor de submuestreo seguro en potencia de 2
            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
            options.inJustDecodeBounds = false
            options.inPreferredConfig = Bitmap.Config.ARGB_8888

            // 3. Decodificar imagen optimizada
            val decodeStream = contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(Exception("No se pudo abrir el flujo de decodificación"))
            val decodedBitmap = BitmapFactory.decodeStream(decodeStream, null, options)
            decodeStream.close()

            if (decodedBitmap == null) {
                return@withContext Result.failure(Exception("Error al decodificar mapa de bits"))
            }

            // 4. Corregir rotación física EXIF si la foto proviene de la cámara del dispositivo
            val rotatedBitmap = correctExifOrientation(context, uri, decodedBitmap)
            Result.success(rotatedBitmap)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Calcula la potencia de 2 óptima para inSampleSize minimizando el consumo de RAM.
     */
    fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return max(1, inSampleSize)
    }

    /**
     * Lee metadatos EXIF del archivo y aplica la rotación afín si fue tomada en vertical/horizontal.
     */
    private fun correctExifOrientation(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                val orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )

                val matrix = Matrix()
                when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                    ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                    ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                    ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                    ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
                    else -> return bitmap
                }

                val transformed = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                if (transformed != bitmap) {
                    bitmap.recycle()
                }
                transformed
            } ?: bitmap
        } catch (e: Exception) {
            bitmap
        }
    }

    /**
     * Genera una miniatura pequeña y nítida para guardado rápido en disco y visualización en listas.
     */
    suspend fun createThumbnail(
        srcBitmap: Bitmap,
        targetSize: Int = 256
    ): Bitmap = withContext(Dispatchers.Default) {
        val width = srcBitmap.width
        val height = srcBitmap.height
        val ratio = width.toFloat() / height.toFloat()

        val (targetW, targetH) = if (ratio > 1f) {
            targetSize to (targetSize / ratio).toInt()
        } else {
            (targetSize * ratio).toInt() to targetSize
        }

        Bitmap.createScaledBitmap(srcBitmap, max(1, targetW), max(1, targetH), true)
    }
}
