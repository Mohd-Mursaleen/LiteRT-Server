package com.litert.server.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.litert.server.data.AppStatus

@Composable
fun DownloadScreen(
    status: AppStatus,
    progressPercent: Float,
    downloadedMb: Float,
    totalMb: Float,
    speedMbps: Float,
    etaSeconds: Int,
    errorMessage: String?,
    onDownload: () -> Unit,
    onRetry: () -> Unit,
    onUseExistingModel: (String) -> Unit = {}
) {
    var showPathDialog by remember { mutableStateOf(false) }
    var customPath by remember { mutableStateOf("") }

    if (showPathDialog) {
        AlertDialog(
            onDismissRequest = { showPathDialog = false },
            containerColor = Color(0xFF1A1A1A),
            title = {
                Text("Enter model path", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        "Paste the full path to your .litertlm file on device storage.",
                        color = Color.Gray,
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = customPath,
                        onValueChange = { customPath = it },
                        placeholder = { Text("/sdcard/Download/gemma-4-E2B-it.litertlm", color = Color.DarkGray, fontSize = 12.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GreenPrimary,
                            unfocusedBorderColor = Color(0xFF444444),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (customPath.isNotBlank()) {
                        onUseExistingModel(customPath.trim())
                        showPathDialog = false
                    }
                }) {
                    Text("Use this file", color = GreenPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPathDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Download,
            contentDescription = null,
            tint = GreenPrimary,
            modifier = Modifier.size(72.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "Gemma 4 E2B",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            "On-device multimodal LLM",
            color = Color.Gray,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "~2.58 GB · One-time download",
            color = Color.Gray,
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.height(40.dp))

        when (status) {
            AppStatus.MODEL_NOT_FOUND -> {
                Button(
                    onClick = onDownload,
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Download Model", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { showPathDialog = true },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Gray),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        // subtle border
                    ),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Use existing model file", fontSize = 14.sp)
                }
            }

            AppStatus.DOWNLOADING -> {
                LinearProgressIndicator(
                    progress = { progressPercent },
                    modifier = Modifier.fillMaxWidth(),
                    color = GreenPrimary,
                    trackColor = Color(0xFF333333)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "${(progressPercent * 100).toInt()}%",
                    color = GreenPrimary,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "${"%.1f".format(downloadedMb)} MB / ${"%.0f".format(totalMb)} MB",
                    color = Color.White,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "${"%.1f".format(speedMbps)} MB/s · ETA ${etaSeconds}s",
                    color = Color.Gray,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                CircularProgressIndicator(color = GreenPrimary, modifier = Modifier.size(32.dp))
            }

            AppStatus.DOWNLOAD_ERROR -> {
                Text(
                    "Download failed",
                    color = Color(0xFFEF4444),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                errorMessage?.let {
                    Text(it, color = Color.Gray, fontSize = 13.sp, textAlign = TextAlign.Center)
                }
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Retry Download", fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { showPathDialog = true },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Gray),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Use existing model file", fontSize = 14.sp)
                }
            }

            AppStatus.INITIALIZING -> {
                CircularProgressIndicator(color = GreenPrimary, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Loading model into GPU memory...",
                    color = Color.White,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    "This may take 10–30 seconds on first launch",
                    color = Color.Gray,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }

            else -> {}
        }
    }
}
