package com.litert.server.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

@Composable
fun VisionScreen(
    isAnalyzing: Boolean,
    analysisResult: String,
    onAnalyze: (Uri, String) -> Unit,
    onShare: (String) -> Unit
) {
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var prompt by remember { mutableStateOf("Describe what you see in detail") }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { selectedUri = it } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Vision Analysis", color = Color.White, style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))

        // Image picker buttons
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = { galleryLauncher.launch("image/*") },
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceColor),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Image, contentDescription = null, tint = GreenPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Gallery", color = Color.White)
            }
            Button(
                onClick = { /* Camera intent - requires FileProvider setup */ },
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceColor),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null, tint = GreenPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Camera", color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Image preview
        selectedUri?.let { uri ->
            Card(
                modifier = Modifier.fillMaxWidth().height(220.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceColor)
            ) {
                AsyncImage(
                    model = uri,
                    contentDescription = "Selected image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Prompt input
        OutlinedTextField(
            value = prompt,
            onValueChange = { prompt = it },
            label = { Text("Prompt", color = Color.Gray) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = GreenPrimary,
                unfocusedBorderColor = Color.Gray,
                cursorColor = GreenPrimary
            ),
            maxLines = 3
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Analyze button
        Button(
            onClick = {
                selectedUri?.let { onAnalyze(it, prompt) }
            },
            enabled = selectedUri != null && !isAnalyzing,
            colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isAnalyzing) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Analyzing...")
            } else {
                Text("Analyze Image", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Result
        if (analysisResult.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceColor),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Analysis Result", color = GreenPrimary, fontWeight = FontWeight.SemiBold)
                        IconButton(onClick = { onShare(analysisResult) }) {
                            Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.Gray)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(analysisResult, color = Color.White, fontSize = 14.sp, lineHeight = 21.sp)
                }
            }
        }
    }
}
