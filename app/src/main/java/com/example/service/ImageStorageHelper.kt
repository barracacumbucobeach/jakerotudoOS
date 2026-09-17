package com.example.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.math.max

object ImageStorageHelper {

    /**
     * Retorna a URL pública do Supabase Storage para qualquer foto (local ou remota).
     */
    fun obterUrlPublica(caminhoFoto: String, baseUrl: String = com.example.data.local.PreferencesManager.DEFAULT_SUPABASE_URL): String {
        if (caminhoFoto.startsWith("http://") || caminhoFoto.startsWith("https://")) {
            return caminhoFoto
        }
        val fileName = File(caminhoFoto).name
        val cleanUrl = baseUrl.trim().removeSuffix("/")
        return "$cleanUrl/storage/v1/object/public/fotos/$fileName"
    }

    suspend fun salvarFotoComprimida(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            var inputStream: InputStream? = contentResolver.openInputStream(uri) ?: return@withContext null

            // 1. Decodificar dimensões
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()

            val maxDimensao = 1000
            var inSampleSize = 1
            val maiorLado = max(options.outWidth, options.outHeight)
            if (maiorLado > maxDimensao) {
                inSampleSize = maiorLado / maxDimensao
            }

            // 2. Decodificar bitmap com downsampling
            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
            }
            inputStream = contentResolver.openInputStream(uri)
            val bitmapOriginal = BitmapFactory.decodeStream(inputStream, null, decodeOptions)
            inputStream?.close()

            if (bitmapOriginal == null) return@withContext null

            // 3. Escalar se ainda for maior que 1000px
            val width = bitmapOriginal.width
            val height = bitmapOriginal.height
            val scale = if (width > maxDimensao || height > maxDimensao) {
                maxDimensao.toFloat() / max(width, height)
            } else 1.0f

            val bitmapFinal = if (scale < 1.0f) {
                Bitmap.createScaledBitmap(
                    bitmapOriginal,
                    (width * scale).toInt(),
                    (height * scale).toInt(),
                    true
                )
            } else {
                bitmapOriginal
            }

            // 4. Salvar JPEG 60%
            val pastaFotos = File(context.filesDir, "fotos_os").apply { mkdirs() }
            val nomeArquivo = "os_foto_${System.currentTimeMillis()}_${(100..999).random()}.jpg"
            val arquivoDestino = File(pastaFotos, nomeArquivo)

            FileOutputStream(arquivoDestino).use { out ->
                bitmapFinal.compress(Bitmap.CompressFormat.JPEG, 60, out)
            }

            if (bitmapFinal != bitmapOriginal) {
                bitmapFinal.recycle()
            }
            bitmapOriginal.recycle()

            arquivoDestino.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
