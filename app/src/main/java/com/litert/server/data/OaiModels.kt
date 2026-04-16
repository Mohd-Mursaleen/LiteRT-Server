package com.litert.server.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// OpenAI-compatible request/response models

@Serializable
data class OaiChatRequest(
    val model: String = "gemma-4-e2b",
    val messages: List<OaiMessage>,
    val stream: Boolean = false,
    @SerialName("max_tokens") val maxTokens: Int? = null,
    val temperature: Double? = null
)

@Serializable
data class OaiMessage(
    val role: String,
    val content: String
)

@Serializable
data class OaiChatResponse(
    val id: String,
    val `object`: String = "chat.completion",
    val created: Long,
    val model: String,
    val choices: List<OaiChoice>
)

@Serializable
data class OaiChoice(
    val index: Int,
    val message: OaiMessage,
    @SerialName("finish_reason") val finishReason: String
)

@Serializable
data class OaiStreamChunk(
    val id: String,
    val `object`: String = "chat.completion.chunk",
    val created: Long,
    val model: String,
    val choices: List<OaiStreamChoice>
)

@Serializable
data class OaiStreamChoice(
    val index: Int,
    val delta: OaiDelta,
    @SerialName("finish_reason") val finishReason: String? = null
)

@Serializable
data class OaiDelta(
    val role: String? = null,
    val content: String? = null
)

@Serializable
data class OaiModelsResponse(
    val `object`: String = "list",
    val data: List<OaiModelEntry>
)

@Serializable
data class OaiModelEntry(
    val id: String,
    val `object`: String = "model",
    val created: Long = 1_700_000_000L,
    @SerialName("owned_by") val ownedBy: String = "google"
)
