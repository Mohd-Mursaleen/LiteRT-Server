package com.litert.server.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.litert.server.data.ChatMessage
import com.litert.server.data.MessageRole
import kotlinx.coroutines.launch

val GreenPrimary = Color(0xFF4CAF50)
val DarkBackground = Color(0xFF121212)
val SurfaceColor = Color(0xFF1E1E1E)
val BubbleUser = Color(0xFF2E5F31)
val BubbleAI = Color(0xFF2A2A2A)

@Composable
fun ChatScreen(
    messages: List<ChatMessage>,
    isGenerating: Boolean,
    onSend: (String) -> Unit,
    onClear: () -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            scope.launch { listState.animateScrollToItem(messages.size - 1) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Chat", color = Color.White, style = MaterialTheme.typography.titleLarge)
            IconButton(onClick = onClear) {
                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.Gray)
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { msg ->
                MessageBubble(msg)
            }
            if (isGenerating) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Box(
                            modifier = Modifier
                                .background(BubbleAI, RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Text("Thinking...", color = Color.Gray, fontStyle = FontStyle.Italic)
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceColor)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type a message...", color = Color.Gray) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = GreenPrimary,
                    unfocusedBorderColor = Color.Gray,
                    cursorColor = GreenPrimary
                ),
                maxLines = 4
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (inputText.isNotBlank() && !isGenerating) {
                        onSend(inputText.trim())
                        inputText = ""
                    }
                },
                enabled = inputText.isNotBlank() && !isGenerating
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send", tint = GreenPrimary)
            }
        }
    }
}

@Composable
fun MessageBubble(message: ChatMessage) {
    val isUser = message.role == MessageRole.USER
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .background(
                    if (isUser) BubbleUser else BubbleAI,
                    RoundedCornerShape(
                        topStart = 12.dp,
                        topEnd = 12.dp,
                        bottomStart = if (isUser) 12.dp else 2.dp,
                        bottomEnd = if (isUser) 2.dp else 12.dp
                    )
                )
                .padding(12.dp)
        ) {
            Text(
                text = message.content,
                color = Color.White,
                fontSize = 15.sp
            )
            if (!isUser && message.tokenCount > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${message.tokenCount} tokens · ${message.generationTimeMs}ms",
                    color = Color.Gray,
                    fontSize = 11.sp
                )
            }
        }
    }
}
