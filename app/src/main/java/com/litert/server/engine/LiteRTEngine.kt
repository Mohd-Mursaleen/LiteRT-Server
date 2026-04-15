package com.litert.server.engine

import android.content.Context
import android.util.Log
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class LiteRTEngine(private val context: Context) {

    companion object {
        private const val TAG = "LiteRTEngine"
    }

    private var engine: Engine? = null
    private var conversation: com.google.ai.edge.litertlm.Conversation? = null
    private var currentBackend: String = "GPU"

    var isReady = false
        private set

    suspend fun initialize(
        modelPath: String,
        useGpu: Boolean = true,
        temperature: Float = 0.7f,
        maxTokens: Int = 1024,
        topK: Int = 40
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val backend = if (useGpu) Backend.GPU() else Backend.CPU()
                val visionBackend = if (useGpu) Backend.GPU() else Backend.CPU()

                val config = EngineConfig(
                    modelPath = modelPath,
                    backend = backend,
                    visionBackend = visionBackend,
                    cacheDir = context.cacheDir.absolutePath
                )
                val newEngine = Engine(config)
                newEngine.initialize()

                val conv = newEngine.createConversation(
                    ConversationConfig(
                        systemInstruction = Contents.of(
                            Content.Text(
                                "You are a helpful AI assistant running locally on an Android device " +
                                "powered by Google's Gemma 4 multimodal LLM via LiteRT."
                            )
                        ),
                        topK = topK,
                        temperature = temperature,
                        maxTokens = maxTokens
                    )
                )

                engine = newEngine
                conversation = conv
                currentBackend = if (useGpu) "GPU" else "CPU"
                isReady = true
                Log.i(TAG, "Engine initialized with $currentBackend backend")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize with ${if (useGpu) "GPU" else "CPU"} backend", e)
                if (useGpu) {
                    Log.w(TAG, "Falling back to CPU backend...")
                    initialize(modelPath, useGpu = false, temperature, maxTokens, topK)
                } else {
                    isReady = false
                    false
                }
            }
        }
    }

    suspend fun generateText(prompt: String): Flow<String> {
        val conv = conversation ?: throw IllegalStateException("Engine not initialized")
        return conv.sendMessageAsync(
            Contents.of(Content.Text(prompt))
        )
    }

    suspend fun analyzeImage(imagePath: String, prompt: String): Flow<String> {
        val conv = conversation ?: throw IllegalStateException("Engine not initialized")
        return conv.sendMessageAsync(
            Contents.of(
                Content.ImageFile(imagePath),
                Content.Text(prompt)
            )
        )
    }

    fun clearHistory() {
        conversation?.clearHistory()
    }

    fun getBackend(): String = currentBackend

    fun shutdown() {
        isReady = false
        conversation = null
        engine?.close()
        engine = null
    }
}
