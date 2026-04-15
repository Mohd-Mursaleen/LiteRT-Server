package com.litert.server.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsScreen(
    modelPath: String,
    isGpu: Boolean,
    onClearCache: () -> Unit
) {
    var temperature by remember { mutableFloatStateOf(0.7f) }
    var maxTokens by remember { mutableFloatStateOf(1024f) }
    var useGpu by remember { mutableStateOf(isGpu) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Settings", color = Color.White, style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(24.dp))

        SettingsCard {
            Text("Model Path", color = Color.Gray, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(modelPath, color = Color.White, fontSize = 13.sp)
        }

        Spacer(modifier = Modifier.height(12.dp))

        SettingsCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("GPU Acceleration", color = Color.White, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Adreno 630 / OpenCL 2.0",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
                Switch(
                    checked = useGpu,
                    onCheckedChange = { useGpu = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = GreenPrimary, checkedTrackColor = Color(0xFF1A3A1A))
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        SettingsCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Temperature", color = Color.White, fontWeight = FontWeight.SemiBold)
                Text("${"%.2f".format(temperature)}", color = GreenPrimary)
            }
            Slider(
                value = temperature,
                onValueChange = { temperature = it },
                valueRange = 0.1f..1.0f,
                colors = SliderDefaults.colors(
                    thumbColor = GreenPrimary,
                    activeTrackColor = GreenPrimary,
                    inactiveTrackColor = Color(0xFF333333)
                )
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("0.1", color = Color.Gray, fontSize = 11.sp)
                Text("1.0", color = Color.Gray, fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        SettingsCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Max Tokens", color = Color.White, fontWeight = FontWeight.SemiBold)
                Text("${maxTokens.toInt()}", color = GreenPrimary)
            }
            Slider(
                value = maxTokens,
                onValueChange = { maxTokens = it },
                valueRange = 128f..2048f,
                steps = 14,
                colors = SliderDefaults.colors(
                    thumbColor = GreenPrimary,
                    activeTrackColor = GreenPrimary,
                    inactiveTrackColor = Color(0xFF333333)
                )
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("128", color = Color.Gray, fontSize = 11.sp)
                Text("2048", color = Color.Gray, fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = { showDeleteDialog = true },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            border = ButtonDefaults.outlinedButtonBorder.copy(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFEF4444))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Delete Model & Cache")
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("LiteRT Server v1.0", color = Color.Gray, fontSize = 12.sp)
        Text("Gemma 4 E2B · LiteRT-LM SDK 0.10.0", color = Color.Gray, fontSize = 12.sp)
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Model?") },
            text = { Text("This will delete the downloaded model (2.58 GB) and require a re-download.") },
            confirmButton = {
                TextButton(onClick = {
                    onClearCache()
                    showDeleteDialog = false
                }) { Text("Delete", color = Color(0xFFEF4444)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}
