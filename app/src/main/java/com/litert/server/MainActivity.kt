package com.litert.server

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Api
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.litert.server.data.*
import com.litert.server.download.ModelDownloadManager
import com.litert.server.service.LLMForegroundService
import com.litert.server.ui.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var downloadManager: ModelDownloadManager
    private var appState by mutableStateOf(AppState())
    private var chatMessages = mutableStateListOf<ChatMessage>()
    private var isGenerating by mutableStateOf(false)
    private var visionResult by mutableStateOf("")
    private var isAnalyzing by mutableStateOf(false)
    private var selectedTab by mutableIntStateOf(0)

    private val engineReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                LLMForegroundService.ACTION_ENGINE_READY -> {
                    val port = intent.getIntExtra(LLMForegroundService.EXTRA_SERVER_PORT, 8080)
                    val isGpu = intent.getBooleanExtra(LLMForegroundService.EXTRA_IS_GPU, true)
                    appState = appState.copy(
                        status = AppStatus.READY,
                        isServerRunning = true,
                        serverPort = port,
                        isGpuBackend = isGpu,
                        engineReady = true
                    )
                }
                LLMForegroundService.ACTION_ENGINE_ERROR -> {
                    val msg = intent.getStringExtra(LLMForegroundService.EXTRA_ERROR_MESSAGE)
                    appState = appState.copy(status = AppStatus.ERROR, errorMessage = msg)
                }
            }
        }
    }

    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* handle result */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        downloadManager = ModelDownloadManager(this)

        val filter = IntentFilter().apply {
            addAction(LLMForegroundService.ACTION_ENGINE_READY)
            addAction(LLMForegroundService.ACTION_ENGINE_ERROR)
        }
        registerReceiver(engineReceiver, filter, RECEIVER_NOT_EXPORTED)

        // Request POST_NOTIFICATIONS
        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)

        // Request battery optimization exemption
        val pm = getSystemService(PowerManager::class.java)
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:$packageName")
            })
        }

        checkModelAndUpdateState()

        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                AppContent()
            }
        }
    }

    @Composable
    fun AppContent() {
        when (appState.status) {
            AppStatus.MODEL_NOT_FOUND, AppStatus.DOWNLOADING, AppStatus.DOWNLOAD_ERROR, AppStatus.INITIALIZING -> {
                DownloadScreen(
                    status = appState.status,
                    progressPercent = appState.downloadProgress,
                    downloadedMb = appState.downloadedMb,
                    totalMb = appState.totalMb,
                    speedMbps = appState.downloadSpeedMbps,
                    etaSeconds = appState.etaSeconds,
                    errorMessage = appState.errorMessage,
                    onDownload = ::startDownload,
                    onRetry = ::startDownload
                )
            }
            AppStatus.READY -> {
                MainTabLayout()
            }
            AppStatus.ERROR -> {
                Box(modifier = Modifier.fillMaxSize().background(DarkBackground)) {
                    Column(modifier = Modifier.padding(all = androidx.compose.ui.unit.dp.times(24))) {
                        Text(
                            "Error: ${appState.errorMessage}",
                            color = Color(0xFFEF4444)
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun MainTabLayout() {
        val tabs = listOf("Chat", "Vision", "Server", "Settings")
        val icons = listOf(
            Icons.Default.Chat,
            Icons.Default.Image,
            Icons.Default.Api,
            Icons.Default.Settings
        )

        Scaffold(
            containerColor = DarkBackground,
            bottomBar = {
                NavigationBar(containerColor = SurfaceColor) {
                    tabs.forEachIndexed { index, tab ->
                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            icon = { Icon(icons[index], contentDescription = tab) },
                            label = { Text(tab, color = if (selectedTab == index) GreenPrimary else Color.Gray) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = GreenPrimary,
                                unselectedIconColor = Color.Gray,
                                indicatorColor = Color(0xFF1A3A1A)
                            )
                        )
                    }
                }
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding)) {
                when (selectedTab) {
                    0 -> ChatScreen(
                        messages = chatMessages,
                        isGenerating = isGenerating,
                        onSend = ::sendMessage,
                        onClear = { chatMessages.clear() }
                    )
                    1 -> VisionScreen(
                        isAnalyzing = isAnalyzing,
                        analysisResult = visionResult,
                        onAnalyze = { _, _ -> },
                        onShare = { Toast.makeText(this@MainActivity, "Copied!", Toast.LENGTH_SHORT).show() }
                    )
                    2 -> ServerScreen(
                        isRunning = appState.isServerRunning,
                        port = appState.serverPort,
                        requestLog = appState.requestLog,
                        onToggle = ::toggleServer
                    )
                    3 -> SettingsScreen(
                        modelPath = downloadManager.getModelPath(),
                        isGpu = appState.isGpuBackend,
                        onClearCache = { downloadManager.deleteModel(); checkModelAndUpdateState() }
                    )
                }
            }
        }
    }

    private fun checkModelAndUpdateState() {
        appState = if (downloadManager.isModelDownloaded()) {
            appState.copy(status = AppStatus.INITIALIZING)
        } else {
            appState.copy(status = AppStatus.MODEL_NOT_FOUND)
        }
    }

    private fun startDownload() {
        appState = appState.copy(status = AppStatus.DOWNLOADING, errorMessage = null)
        lifecycleScope.launch(Dispatchers.IO) {
            downloadManager.downloadModel()
                .catch { e ->
                    appState = appState.copy(
                        status = AppStatus.DOWNLOAD_ERROR,
                        errorMessage = e.message
                    )
                }
                .collect { progress ->
                    appState = appState.copy(
                        downloadProgress = progress.progressPercent,
                        downloadedMb = progress.downloadedMb,
                        totalMb = progress.totalMb,
                        downloadSpeedMbps = progress.speedMbps,
                        etaSeconds = progress.etaSeconds
                    )
                    if (progress.isDone) {
                        startEngineService()
                    }
                }
        }
    }

    private fun startEngineService() {
        appState = appState.copy(status = AppStatus.INITIALIZING)
        val intent = Intent(this, LLMForegroundService::class.java).apply {
            putExtra(LLMForegroundService.EXTRA_MODEL_PATH, downloadManager.getModelPath())
            putExtra(LLMForegroundService.EXTRA_USE_GPU, true)
        }
        startForegroundService(intent)
    }

    private fun toggleServer() {
        if (appState.isServerRunning) {
            stopService(Intent(this, LLMForegroundService::class.java))
            appState = appState.copy(isServerRunning = false, engineReady = false)
        } else {
            startEngineService()
        }
    }

    private fun sendMessage(text: String) {
        // Delegate to LLMForegroundService via bound service or HTTP
        chatMessages.add(ChatMessage(role = MessageRole.USER, content = text))
        Toast.makeText(this, "Engine must be running to chat", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        unregisterReceiver(engineReceiver)
        super.onDestroy()
    }
}
