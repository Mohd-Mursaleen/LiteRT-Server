package com.litert.server.download

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

data class DownloadProgress(
    val progressPercent: Float,
    val downloadedMb: Float,
    val totalMb: Float,
    val speedMbps: Float,
    val etaSeconds: Int,
    val isDone: Boolean = false,
    val error: String? = null
)

/**
 * All Gemma 4 variants available as .litertlm from litert-community on HuggingFace.
 */
enum class GemmaVariant(
    val displayName: String,
    val description: String,
    val sizeGb: Float,
    val filename: String,
    val url: String,
    val minValidBytes: Long
) {
    E2B(
        displayName = "Gemma 4 E2B",
        description = "2B MoE · multimodal (text, image, audio) · 128K ctx",
        sizeGb = 2.58f,
        filename = "gemma-4-E2B-it.litertlm",
        url = "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it.litertlm",
        minValidBytes = 2_400_000_000L
    ),
    E4B(
        displayName = "Gemma 4 E4B",
        description = "4B MoE · multimodal (text, image, audio) · 128K ctx",
        sizeGb = 3.65f,
        filename = "gemma-4-E4B-it.litertlm",
        url = "https://huggingface.co/litert-community/gemma-4-E4B-it-litert-lm/resolve/main/gemma-4-E4B-it.litertlm",
        minValidBytes = 3_500_000_000L
    )
    // 26B A4B and 31B are not available as .litertlm for mobile yet
}

class ModelDownloadManager(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    private var activeVariant: GemmaVariant = GemmaVariant.E2B

    fun setVariant(variant: GemmaVariant) {
        activeVariant = variant
    }

    fun getActiveVariant(): GemmaVariant = activeVariant

    fun getModelPath(): String =
        "${context.getExternalFilesDir(null)?.absolutePath}/${activeVariant.filename}"

    fun isModelDownloaded(): Boolean {
        val file = File(getModelPath())
        return file.exists() && file.length() >= activeVariant.minValidBytes
    }

    /**
     * Called after user picks a file via the system file picker.
     * Copies (or symlinks via path) the file to our expected model location.
     */
    fun useExistingModel(sourcePath: String): Boolean {
        return try {
            val src = File(sourcePath)
            if (!src.exists() || src.length() < 100_000_000L) return false
            val dest = File(getModelPath())
            src.copyTo(dest, overwrite = true)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun downloadModel(): Flow<DownloadProgress> = flow {
        val variant = activeVariant
        val destFile = File(getModelPath())
        val existingBytes = if (destFile.exists()) destFile.length() else 0L

        val requestBuilder = Request.Builder()
            .url(variant.url)
            .header("User-Agent", "LiteRT-Server-Android/1.0")
        if (existingBytes > 0) {
            requestBuilder.header("Range", "bytes=$existingBytes-")
        }

        val response = client.newCall(requestBuilder.build()).execute()

        if (!response.isSuccessful && response.code != 206) {
            throw Exception("Download failed: HTTP ${response.code} — ${response.message}")
        }

        val totalBytes = when {
            response.code == 206 -> {
                val contentRange = response.header("Content-Range") ?: ""
                contentRange.substringAfterLast('/').toLongOrNull()
                    ?: (variant.sizeGb * 1024 * 1024 * 1024).toLong()
            }
            else -> response.body?.contentLength()?.takeIf { it > 0 }
                ?: (variant.sizeGb * 1024 * 1024 * 1024).toLong()
        }

        val body = response.body ?: throw Exception("Empty response body")
        val buffer = ByteArray(8192)
        var downloadedBytes = existingBytes
        var lastSpeedTime = System.currentTimeMillis()
        var lastSpeedBytes = downloadedBytes

        val outputStream = FileOutputStream(destFile, existingBytes > 0)

        body.byteStream().use { inputStream ->
            outputStream.use { outStream ->
                while (true) {
                    val read = inputStream.read(buffer)
                    if (read == -1) break
                    outStream.write(buffer, 0, read)
                    downloadedBytes += read

                    val now = System.currentTimeMillis()
                    val elapsed = now - lastSpeedTime
                    if (elapsed >= 1000) {
                        val bytesSinceLastCheck = downloadedBytes - lastSpeedBytes
                        val speedMbps = (bytesSinceLastCheck / 1024f / 1024f) / (elapsed / 1000f)
                        val remaining = totalBytes - downloadedBytes
                        val etaSec = if (speedMbps > 0) (remaining / 1024 / 1024 / speedMbps).toInt() else 0
                        emit(
                            DownloadProgress(
                                progressPercent = downloadedBytes.toFloat() / totalBytes,
                                downloadedMb = downloadedBytes / 1024f / 1024f,
                                totalMb = totalBytes / 1024f / 1024f,
                                speedMbps = speedMbps,
                                etaSeconds = etaSec
                            )
                        )
                        lastSpeedTime = now
                        lastSpeedBytes = downloadedBytes
                    }
                }
            }
        }

        if (destFile.length() < variant.minValidBytes) {
            destFile.delete()
            throw Exception("Downloaded file too small — may be corrupted. Please retry.")
        }

        emit(
            DownloadProgress(
                progressPercent = 1f,
                downloadedMb = destFile.length() / 1024f / 1024f,
                totalMb = totalBytes / 1024f / 1024f,
                speedMbps = 0f,
                etaSeconds = 0,
                isDone = true
            )
        )
    }.flowOn(Dispatchers.IO)

    fun deleteModel() {
        File(getModelPath()).delete()
    }
}
