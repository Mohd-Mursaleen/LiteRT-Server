package com.litert.server.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
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
import com.litert.server.download.GemmaVariant

@Composable
fun DownloadScreen(
    status: AppStatus,
    progressPercent: Float,
    downloadedMb: Float,
    totalMb: Float,
    speedMbps: Float,
    etaSeconds: Int,
    errorMessage: String?,
    selectedVariant: GemmaVariant,
    onVariantSelected: (GemmaVariant) -> Unit,
    onDownload: () -> Unit,
    onRetry: () -> Unit,
    onPickFile: () -> Unit,
    onUseExistingModel: (String) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Download,
            contentDescription = null,
            tint = GreenPrimary,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            "LiteRT Server",
            color = Color.White,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            "On-device LLM via Google LiteRT-LM",
            color = Color.Gray,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))

        // ── Model selector ────────────────────────────────────────────────
        if (status == AppStatus.MODEL_NOT_FOUND || status == AppStatus.DOWNLOAD_ERROR) {
            Text(
                "Select model",
                color = Color.Gray,
                fontSize = 12.sp,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(8.dp))
            GemmaVariant.entries.forEach { variant ->
                val selected = variant == selectedVariant
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .border(
                            width = if (selected) 1.5.dp else 1.dp,
                            color = if (selected) GreenPrimary else Color(0xFF333333),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .background(
                            color = if (selected) Color(0xFF0D2D0D) else Color(0xFF111111),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable { onVariantSelected(variant) }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            variant.displayName,
                            color = if (selected) GreenPrimary else Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            variant.description,
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
                    }
                    Text(
                        "${variant.sizeGb} GB",
                        color = if (selected) GreenPrimary else Color.Gray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    if (selected) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = GreenPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        // ── Status-based content ──────────────────────────────────────────
        when (status) {
            AppStatus.MODEL_NOT_FOUND -> {
                Button(
                    onClick = onDownload,
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Download ${selectedVariant.displayName}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onPickFile,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.LightGray),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        Icons.Default.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Browse for .litertlm file", fontSize = 14.sp)
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
                Spacer(modifier = Modifier.height(6.dp))
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
                    Text(
                        it,
                        color = Color.Gray,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Retry Download", fontSize = 15.sp)
                }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onPickFile,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.LightGray),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        Icons.Default.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Browse for .litertlm file", fontSize = 14.sp)
                }
            }

            AppStatus.INITIALIZING -> {
                CircularProgressIndicator(
                    color = GreenPrimary,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Loading model into GPU memory...",
                    color = Color.White,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    "This may take 10–60 seconds on first launch",
                    color = Color.Gray,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }

            else -> {}
        }
    }
}
