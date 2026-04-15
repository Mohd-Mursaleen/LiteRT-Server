package com.litert.server.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.ServiceCompat
import com.litert.server.data.RequestLogEntry
import com.litert.server.engine.LiteRTEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class LLMForegroundService : Service() {

    companion object {
        private const val TAG = "LLMForegroundService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "litert_server_channel"
        const val EXTRA_MODEL_PATH = "model_path"
        const val EXTRA_USE_GPU = "use_gpu"
        const val ACTION_ENGINE_READY = "com.litert.server.ENGINE_READY"
        const val ACTION_ENGINE_ERROR = "com.litert.server.ENGINE_ERROR"
        const val EXTRA_ERROR_MESSAGE = "error_message"
        const val EXTRA_SERVER_PORT = "server_port"
        const val EXTRA_IS_GPU = "is_gpu"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var llmEngine: LiteRTEngine? = null
    private var apiServer: HttpApiServer? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val modelPath = intent?.getStringExtra(EXTRA_MODEL_PATH) ?: return START_NOT_STICKY
        val useGpu = intent.getBooleanExtra(EXTRA_USE_GPU, true)

        startAsForeground()

        scope.launch {
            try {
                val engine = LiteRTEngine(applicationContext)
                llmEngine = engine

                val success = engine.initialize(modelPath, useGpu)
                if (!success) {
                    broadcastError("Failed to initialize LLM engine")
                    return@launch
                }

                val requestLog = mutableListOf<RequestLogEntry>()
                val server = HttpApiServer(engine) { entry ->
                    synchronized(requestLog) { requestLog.add(entry) }
                }
                val port = server.start()
                apiServer = server

                updateNotification("LiteRT Server Running — localhost:$port")
                broadcastReady(port, engine.getBackend() == "GPU")
            } catch (e: Exception) {
                Log.e(TAG, "Service error", e)
                broadcastError(e.message ?: "Unknown error")
            }
        }

        return START_STICKY
    }

    private fun startAsForeground() {
        val notification = buildNotification("LiteRT Server — Starting...")
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification,
            android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "LiteRT Server",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "LLM inference and HTTP API server"
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(text: String): Notification =
        Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("LiteRT Server")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build()

    private fun updateNotification(text: String) {
        val notification = buildNotification(text)
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun broadcastReady(port: Int, isGpu: Boolean) {
        val intent = Intent(ACTION_ENGINE_READY).apply {
            putExtra(EXTRA_SERVER_PORT, port)
            putExtra(EXTRA_IS_GPU, isGpu)
        }
        sendBroadcast(intent)
    }

    private fun broadcastError(message: String) {
        val intent = Intent(ACTION_ENGINE_ERROR).apply {
            putExtra(EXTRA_ERROR_MESSAGE, message)
        }
        sendBroadcast(intent)
    }

    override fun onDestroy() {
        apiServer?.stop()
        llmEngine?.shutdown()
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
