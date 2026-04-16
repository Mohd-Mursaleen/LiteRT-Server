package com.litert.server.service

import com.litert.server.data.ChatRequest
import com.litert.server.data.ChatResponse
import com.litert.server.data.ErrorResponse
import com.litert.server.data.HealthResponse
import com.litert.server.data.OaiChatRequest
import com.litert.server.data.OaiChatResponse
import com.litert.server.data.OaiChoice
import com.litert.server.data.OaiDelta
import com.litert.server.data.OaiMessage
import com.litert.server.data.OaiModelEntry
import com.litert.server.data.OaiModelsResponse
import com.litert.server.data.OaiStreamChunk
import com.litert.server.data.OaiStreamChoice
import com.litert.server.data.RequestLogEntry
import com.litert.server.data.VisionRequest
import com.litert.server.engine.LiteRTEngine
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondTextWriter
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class HttpApiServer(
    private val engine: LiteRTEngine,
    private val onRequest: (RequestLogEntry) -> Unit
) {
    private var server: ApplicationEngine? = null
    var port: Int = 8080
        private set

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun start(): Int {
        for (tryPort in 8080..8082) {
            try {
                server = embeddedServer(CIO, port = tryPort) {
                    install(ContentNegotiation) {
                        json(json)
                    }
                    install(CORS) {
                        anyHost()
                    }
                    install(StatusPages) {
                        exception<Throwable> { call, cause ->
                            call.respond(
                                HttpStatusCode.InternalServerError,
                                ErrorResponse(error = cause.message ?: "Unknown error", code = 500)
                            )
                        }
                    }

                    routing {

                        // ── health ──────────────────────────────────────────
                        get("/health") {
                            call.respond(
                                HealthResponse(
                                    status = "ok",
                                    model = "gemma-4-E2B",
                                    gpu = engine.getBackend() == "GPU",
                                    ready = engine.isReady
                                )
                            )
                        }

                        // ── OpenAI-compatible v1 routes ──────────────────────
                        route("/v1") {

                            get("/models") {
                                call.respond(
                                    OaiModelsResponse(
                                        data = listOf(
                                            OaiModelEntry(id = "gemma-4-e2b")
                                        )
                                    )
                                )
                            }

                            post("/chat/completions") {
                                if (!engine.isReady) {
                                    call.respond(
                                        HttpStatusCode.ServiceUnavailable,
                                        ErrorResponse("Engine not ready", 503)
                                    )
                                    return@post
                                }

                                val req = call.receive<OaiChatRequest>()
                                val start = System.currentTimeMillis()

                                // Build prompt from message history
                                val prompt = req.messages.joinToString("\n") {
                                    "${it.role}: ${it.content}"
                                } + "\nassistant:"

                                if (req.stream) {
                                    val reqId = "chatcmpl-${System.currentTimeMillis()}"
                                    call.respondTextWriter(contentType = ContentType.Text.EventStream) {
                                        // First chunk carries the role
                                        val firstChunk = OaiStreamChunk(
                                            id = reqId,
                                            created = System.currentTimeMillis() / 1000,
                                            model = "gemma-4-e2b",
                                            choices = listOf(
                                                OaiStreamChoice(
                                                    index = 0,
                                                    delta = OaiDelta(role = "assistant", content = "")
                                                )
                                            )
                                        )
                                        write("data: ${json.encodeToString(firstChunk)}\n\n")
                                        flush()

                                        engine.generateText(prompt).collect { token ->
                                            val chunk = OaiStreamChunk(
                                                id = reqId,
                                                created = System.currentTimeMillis() / 1000,
                                                model = "gemma-4-e2b",
                                                choices = listOf(
                                                    OaiStreamChoice(
                                                        index = 0,
                                                        delta = OaiDelta(content = token)
                                                    )
                                                )
                                            )
                                            write("data: ${json.encodeToString(chunk)}\n\n")
                                            flush()
                                        }

                                        // Final stop chunk
                                        val stopChunk = OaiStreamChunk(
                                            id = reqId,
                                            created = System.currentTimeMillis() / 1000,
                                            model = "gemma-4-e2b",
                                            choices = listOf(
                                                OaiStreamChoice(
                                                    index = 0,
                                                    delta = OaiDelta(),
                                                    finishReason = "stop"
                                                )
                                            )
                                        )
                                        write("data: ${json.encodeToString(stopChunk)}\n\n")
                                        write("data: [DONE]\n\n")
                                        flush()
                                    }
                                    val ms = System.currentTimeMillis() - start
                                    onRequest(RequestLogEntry(endpoint = "/v1/chat/completions", responseTimeMs = ms, statusCode = 200))
                                } else {
                                    val tokens = engine.generateText(prompt).toList()
                                    val content = tokens.joinToString("")
                                    val ms = System.currentTimeMillis() - start
                                    onRequest(RequestLogEntry(endpoint = "/v1/chat/completions", responseTimeMs = ms, statusCode = 200))
                                    call.respond(
                                        OaiChatResponse(
                                            id = "chatcmpl-${System.currentTimeMillis()}",
                                            created = System.currentTimeMillis() / 1000,
                                            model = "gemma-4-e2b",
                                            choices = listOf(
                                                OaiChoice(
                                                    index = 0,
                                                    message = OaiMessage(role = "assistant", content = content),
                                                    finishReason = "stop"
                                                )
                                            )
                                        )
                                    )
                                }
                            }
                        }

                        // ── Legacy routes (kept for backward compat) ─────────
                        post("/chat") {
                            val start = System.currentTimeMillis()
                            val req = call.receive<ChatRequest>()
                            if (!engine.isReady) {
                                call.respond(
                                    HttpStatusCode.ServiceUnavailable,
                                    ErrorResponse("Engine not ready", 503)
                                )
                                return@post
                            }
                            val tokens = engine.generateText(req.message).toList()
                            val response = tokens.joinToString("")
                            val ms = System.currentTimeMillis() - start
                            onRequest(RequestLogEntry(endpoint = "/chat", responseTimeMs = ms, statusCode = 200))
                            call.respond(
                                ChatResponse(response = response, tokens = tokens.size, ms = ms)
                            )
                        }

                        post("/vision") {
                            val start = System.currentTimeMillis()
                            val req = call.receive<VisionRequest>()
                            if (!engine.isReady) {
                                call.respond(
                                    HttpStatusCode.ServiceUnavailable,
                                    ErrorResponse("Engine not ready", 503)
                                )
                                return@post
                            }
                            val tokens = engine.analyzeImage(req.imagePath, req.prompt).toList()
                            val response = tokens.joinToString("")
                            val ms = System.currentTimeMillis() - start
                            onRequest(RequestLogEntry(endpoint = "/vision", responseTimeMs = ms, statusCode = 200))
                            call.respond(
                                ChatResponse(response = response, tokens = tokens.size, ms = ms)
                            )
                        }

                        post("/reset") {
                            engine.clearHistory()
                            onRequest(RequestLogEntry(endpoint = "/reset", responseTimeMs = 0, statusCode = 200))
                            call.respond(mapOf("status" to "conversation cleared"))
                        }
                    }
                }
                server!!.start(wait = false)
                port = tryPort
                return tryPort
            } catch (e: Exception) {
                if (tryPort == 8082) throw e
            }
        }
        throw IllegalStateException("Could not bind to any port (8080-8082)")
    }

    fun stop() {
        server?.stop(1000, 5000)
        server = null
    }
}
