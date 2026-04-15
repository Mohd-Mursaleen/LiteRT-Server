package com.litert.server.download

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
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

class ModelDownloadManager(private val context: Context) {

    companion object {
        private const val MODEL_URL =
            "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it-litert-lm.litertlm"
        private const val EXPECTED_SIZE_BYTES = 2_771_820_544L // ~2.58 GB
        private const val MIN_VALID_SIZE = 2_500_000_000L
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    fun getModelPath(): String =
        "${context.getExternalFilesDir(null)?.absolutePath}/gemma4.litertlm"

    fun isModelDownloaded(): Boolean {
        val file = File(getModelPath())
        return file.exists() && file.length() >= MIN_VALID_SIZE
    }

    fun downloadModel(): Flow<DownloadProgress> = flow {
        val destFile = File(getModelPath())
        val existingBytes = if (destFile.exists()) destFile.length() else 0L

        val requestBuilder = Request.Builder().url(MODEL_URL)
        if (existingBytes > 0) {
            requestBuilder.header("Range", "bytes=$existingBytes-")
        }

        withContext(Dispatchers.IO) {
            val response = client.newCall(requestBuilder.build()).execute()

            if (!response.isSuccessful && response.code != 206) {
                throw Exception("Download failed: HTTP ${response.code}")
            }

            val totalBytes = when {
                response.code == 206 -> {
                    val contentRange = response.header("Content-Range") ?: ""
                    contentRange.substringAfterLast('/').toLongOrNull() ?: EXPECTED_SIZE_BYTES
                }
                else -> response.body?.contentLength()?.takeIf { it > 0 } ?: EXPECTED_SIZE_BYTES
            }

            val body = response.body ?: throw Exception("Empty response body")
            val buffer = ByteArray(8192)
            var downloadedBytes = existingBytes
            val startTime = System.currentTimeMillis()
            var lastSpeedTime = startTime
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

            if (destFile.length() < MIN_VALID_SIZE) {
                destFile.delete()
                throw Exception("Downloaded file is too small — may be corrupted. Please retry.")
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
        }
    }

    fun deleteModel() {
        File(getModelPath()).delete()
    }
}
