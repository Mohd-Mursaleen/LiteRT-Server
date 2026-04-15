package com.litert.server.service

import com.litert.server.data.ChatRequest
import com.litert.server.data.ChatResponse
import com.litert.server.data.ErrorResponse
import com.litert.server.data.HealthResponse
import com.litert.server.data.RequestLogEntry
import com.litert.server.data.VisionRequest
import com.litert.server.engine.LiteRTEngine
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
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.json.Json

class HttpApiServer(
    private val engine: LiteRTEngine,
    private val onRequest: (RequestLogEntry) -> Unit
) {
    private var server: ApplicationEngine? = null
    var port: Int = 8080
        private set

    fun start(): Int {
        for (tryPort in 8080..8082) {
            try {
                server = embeddedServer(CIO, port = tryPort) {
                    install(ContentNegotiation) {
                        json(Json { ignoreUnknownKeys = true })
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
