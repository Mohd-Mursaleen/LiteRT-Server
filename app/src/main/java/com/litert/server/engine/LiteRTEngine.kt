package com.litert.server.engine

import android.content.Context
import android.util.Log
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.SamplerConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class LiteRTEngine(private val context: Context) {

    companion object {
        private const val TAG = "LiteRTEngine"
    }

    private var engine: Engine? = null
    private var conversation: com.google.ai.edge.litertlm.Conversation? = null
    private var currentBackend: String = "GPU"
    private var currentSamplerConfig: SamplerConfig = SamplerConfig(topK = 40, temperature = 0.7f)

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

                currentSamplerConfig = SamplerConfig(topK = topK, temperature = temperature)
                val conv = createNewConversation(newEngine, currentSamplerConfig)

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

    private fun createNewConversation(
        eng: Engine,
        samplerConfig: SamplerConfig
    ): com.google.ai.edge.litertlm.Conversation {
        return eng.createConversation(
            ConversationConfig(
                systemInstruction = Contents.of(
                    Content.Text(
                        "You are a helpful AI assistant running locally on an Android device " +
                        "powered by Google's Gemma multimodal LLM via LiteRT."
                    )
                ),
                samplerConfig = samplerConfig
            )
        )
    }

    suspend fun generateText(prompt: String): Flow<String> {
        val conv = conversation ?: throw IllegalStateException("Engine not initialized")
        return conv.sendMessageAsync(prompt).map { it.toString() }
    }

    suspend fun analyzeImage(imagePath: String, prompt: String): Flow<String> {
        val conv = conversation ?: throw IllegalStateException("Engine not initialized")
        // Send image path + prompt as combined text (LiteRT multimodal)
        return conv.sendMessageAsync("[Image: $imagePath] $prompt").map { it.toString() }
    }

    fun clearHistory() {
        val eng = engine ?: return
        conversation?.close()
        conversation = createNewConversation(eng, currentSamplerConfig)
        Log.i(TAG, "Conversation history cleared")
    }

    fun getBackend(): String = currentBackend

    fun shutdown() {
        isReady = false
        conversation?.close()
        conversation = null
        engine?.close()
        engine = null
    }
}
