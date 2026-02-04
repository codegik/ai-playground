# Real-Time Audio Transcription System

A Scala-based real-time audio transcription system that runs **100% locally** without requiring any paid APIs. Uses OpenAI's Whisper model (open-source) for speech recognition with automatic language detection.

## Features

✅ **Real-time transcription** - Stream audio from your microphone and get instant text output  
✅ **Local execution** - No cloud APIs, no internet required, no costs  
✅ **Automatic language detection** - Whisper can detect and transcribe 99+ languages  
✅ **High accuracy** - Based on OpenAI's state-of-the-art Whisper model  
✅ **Privacy-first** - All processing happens on your machine  

## Requirements

- **JDK 25** (required)
- **SBT** (Scala Build Tool)
- **Microphone** (for audio input)
- **Whisper Model** (download once, ~75MB to ~3GB depending on model)

## Quick Start

### 1. Download a Whisper Model

First, create a models directory and download a model:

```bash
mkdir -p models
cd models

# Download the base model (recommended for most use cases - ~142 MB)
wget https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base.bin

# Or choose a different model:
# Tiny model (fastest, ~75 MB):
# wget https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-tiny.bin

# Small model (better accuracy, ~466 MB):
# wget https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-small.bin

# Medium model (high accuracy, ~1.5 GB):
# wget https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-medium.bin

cd ..
```

### 2. Run the Application

```bash
# Using default model path (models/ggml-base.bin)
sbt run

# Or specify a custom model path
sbt "run models/ggml-tiny.bin"
```

### 3. Start Speaking

Once the application starts, speak into your microphone. You'll see transcriptions appear in real-time:

```
============================================================
Real-Time Audio Transcription System
============================================================
Loading Whisper model from: models/ggml-base.bin
✓ Whisper model loaded successfully
✓ Microphone opened: 16000.0Hz, 16-bit, 1 channel(s)
✓ Engine initialized successfully
✓ Microphone capture started
✓ Real-time transcription started (processing 3000ms chunks)
Speak into your microphone...
------------------------------------------------------------

Press Ctrl+C to stop transcription

14:23:45      [en] | Hello, this is a test of the transcription system.
14:23:51      [en] | It works really well and processes audio in real time.
```

## Model Selection Guide

| Model | Size | Speed | Accuracy | Recommendation |
|-------|------|-------|----------|----------------|
| **tiny** | 75 MB | ⚡⚡⚡⚡ | ⭐⭐ | Quick testing |
| **base** | 142 MB | ⚡⚡⚡ | ⭐⭐⭐ | **Best balance** |
| **small** | 466 MB | ⚡⚡ | ⭐⭐⭐⭐ | High accuracy needs |
| **medium** | 1.5 GB | ⚡ | ⭐⭐⭐⭐⭐ | Professional use |
| **large** | 2.9 GB | 🐌 | ⭐⭐⭐⭐⭐ | Best possible quality |

**Recommendation**: Start with `ggml-base.bin` - it provides excellent accuracy with reasonable speed for real-time use.

## Architecture

```
┌─────────────────┐
│   Microphone    │  ← System microphone
└────────┬────────┘
         │ 16kHz PCM audio
         ▼
┌─────────────────┐
│ MicrophoneCapture│  ← Captures audio in chunks
└────────┬────────┘
         │ Audio chunks (3 seconds)
         ▼
┌─────────────────┐
│ WhisperTranscriber│ ← Local Whisper model
└────────┬────────┘
         │ Transcribed text
         ▼
┌─────────────────┐
│   Console Output │  ← Real-time display
└─────────────────┘
```

## Configuration

You can customize the transcription behavior in `RealtimeTranscriptionEngine`:

```scala
val engine = RealtimeTranscriptionEngine(
  modelPath = "models/ggml-base.bin",
  chunkDurationMs = 3000,        // Process audio every 3 seconds
  silenceThreshold = 0.01f       // Silence detection threshold
)
```

## Supported Languages

Whisper automatically detects and transcribes 99+ languages including:

- English, Spanish, French, German, Italian, Portuguese
- Chinese (Mandarin), Japanese, Korean
- Russian, Arabic, Hindi, Turkish
- And many more...

No configuration needed - language detection is automatic!

## Performance Tips

1. **Model Selection**: Use `tiny` or `base` for real-time performance
2. **Chunk Duration**: Smaller chunks (1-2s) = faster response, larger chunks (3-5s) = better accuracy
3. **Hardware**: Works on CPU, but faster on machines with better processors
4. **Memory**: Ensure you have enough RAM for your chosen model

## Troubleshooting

### "Microphone not supported"
- Check that your microphone is connected and working
- Try listing available audio devices: `arecord -l` (Linux) or check System Preferences (macOS)

### "Model file not found"
- Ensure you've downloaded the model to the correct path
- Verify the file exists: `ls -lh models/`

### Slow transcription
- Try a smaller model (tiny or base)
- Increase `chunkDurationMs` for less frequent processing
- Close other resource-intensive applications

### Poor accuracy
- Use a larger model (small, medium, or large)
- Ensure good microphone quality and minimal background noise
- Increase chunk duration for more context

## Technology Stack

- **Language**: Scala 3.5.2
- **JDK**: Java 25
- **Build Tool**: SBT
- **AI Model**: OpenAI Whisper (via whisper.cpp and WhisperJNI)
- **Audio**: Java Sound API

## License

This project uses:
- **Whisper** (MIT License) - OpenAI
- **whisper.cpp** (MIT License) - Georgi Gerganov
- **WhisperJNI** (MIT License) - givimad

## Contributing

Feel free to submit issues or pull requests!

## Future Enhancements

- [ ] Save transcriptions to file
- [ ] Multiple output formats (JSON, SRT subtitles)
- [ ] Voice activity detection (VAD) for better silence handling
- [ ] Support for audio file input
- [ ] Web interface for remote access
- [ ] Speaker diarization (identify different speakers)
