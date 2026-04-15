package com.litert.server.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.litert.server.data.RequestLogEntry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ServerScreen(
    isRunning: Boolean,
    port: Int,
    requestLog: List<RequestLogEntry>,
    onToggle: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp)
    ) {
        Text("Server Control", color = Color.White, style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(24.dp))

        // Status card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceColor),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(if (isRunning) GreenPrimary else Color(0xFFEF4444))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (isRunning) "Running" else "Stopped",
                        color = if (isRunning) GreenPrimary else Color(0xFFEF4444),
                        fontWeight = FontWeight.SemiBold
                    )
                }
                if (isRunning) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "API available at http://localhost:$port",
                        color = Color.Gray,
                        fontSize = 13.sp
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onToggle,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRunning) Color(0xFFEF4444) else GreenPrimary
                    ),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(if (isRunning) "STOP SERVER" else "START SERVER", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Curl examples
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceColor),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Termux curl examples", color = Color.White, fontWeight = FontWeight.SemiBold)
                    IconButton(onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("curl", curlExample(port))
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Copied!", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = GreenPrimary)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    curlExample(port),
                    color = Color(0xFF9CCC65),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Request log
        Text("Recent Requests", color = Color.White, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))

        if (requestLog.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().height(100.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No requests yet", color = Color.Gray, fontSize = 13.sp)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(requestLog.takeLast(20).reversed()) { entry ->
                    RequestLogRow(entry)
                }
            }
        }
    }
}

@Composable
fun RequestLogRow(entry: RequestLogEntry) {
    val fmt = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(fmt.format(Date(entry.timestamp)), color = Color.Gray, fontSize = 12.sp)
        Text(entry.endpoint, color = GreenPrimary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        Text("${entry.responseTimeMs}ms", color = Color.White, fontSize = 12.sp)
        Text(entry.statusCode.toString(), color = Color(0xFF9CCC65), fontSize = 12.sp)
    }
}

private fun curlExample(port: Int) = """
curl -X POST http://localhost:$port/chat \
  -H "Content-Type: application/json" \
  -d '{"message":"Hello!"}'

curl http://localhost:$port/health

curl -X POST http://localhost:$port/vision \
  -H "Content-Type: application/json" \
  -d '{"imagePath":"/sdcard/photo.jpg","prompt":"Describe this"}'

curl -X POST http://localhost:$port/reset
""".trimIndent()
