# LiteRT Server — Android Studio Project

A complete native Android application in Kotlin that runs Google's Gemma 4 E2B multimodal LLM
locally on-device via Google's LiteRT-LM SDK.

## Requirements

- Android Studio Ladybug (2024.2.1) or newer
- Android SDK 35 (Android 15)
- JDK 17
- Target device: Android 8.0+ (API 26+) — tested on OnePlus 6 / Snapdragon 845 / Adreno 630

## Project Structure

```
app/src/main/java/com/litert/server/
├── MainActivity.kt              — Entry point, navigation, state management
├── data/AppState.kt             — All data classes and app state enum
├── download/ModelDownloadManager.kt  — HuggingFace model download with resume + progress
├── engine/LiteRTEngine.kt       — LiteRT-LM SDK wrapper (GPU/CPU backend)
├── service/
│   ├── LLMForegroundService.kt  — Android foreground service (START_STICKY)
│   └── HttpApiServer.kt         — Ktor CIO embedded HTTP server on port 8080
└── ui/
    ├── ChatScreen.kt            — Text chat with streaming tokens
    ├── VisionScreen.kt          — Image + text analysis
    ├── ServerScreen.kt          — Server control panel + request log + curl examples
    ├── DownloadScreen.kt        — Model download UI with progress
    └── SettingsScreen.kt        — GPU toggle, temperature, max tokens, model management
```

## Setup

1. Clone / open this folder in Android Studio
2. Let Gradle sync (it will download ~200MB of dependencies)
3. Build and install on your device: `./gradlew installDebug`
4. On first launch the app will prompt you to download the model (~2.58 GB from HuggingFace)

## HTTP API (Ktor on localhost:8080)

Once the model is loaded and the server is running:

```bash
# Health check
curl http://localhost:8080/health

# Chat
curl -X POST http://localhost:8080/chat \
  -H "Content-Type: application/json" \
  -d '{"message":"Hello!"}'

# Vision (image analysis)
curl -X POST http://localhost:8080/vision \
  -H "Content-Type: application/json" \
  -d '{"imagePath":"/sdcard/DCIM/photo.jpg","prompt":"Describe this image"}'

# Reset conversation history
curl -X POST http://localhost:8080/reset
```

## GPU Acceleration

The app uses the Adreno 630's OpenCL 2.0 support via LiteRT-LM's GPU backend.
`libOpenCL.so` and `libvndksupport.so` are declared in the manifest.
If GPU init fails, the engine automatically falls back to CPU.

## Model

- **Model**: Gemma 4 E2B Instruction-tuned (LiteRT format)
- **URL**: `https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm`
- **Size**: ~2.58 GB
- **Saved to**: `[ExternalFilesDir]/gemma4.litertlm`
- **Resume**: Partial downloads are resumed automatically using HTTP Range headers

## Android 15 Notes

- Foreground service type: `specialUse|dataSync` (required by API 35)
- `POST_NOTIFICATIONS` requested at runtime
- Battery optimization exemption requested on first launch
- `ServiceCompat.startForeground()` used with correct type flags

## APK Link

https://drive.google.com/file/d/147EVwUyKYFmUYRys2-xXf1qiRUDXqL50/view?usp=sharing
