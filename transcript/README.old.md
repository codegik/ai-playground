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

### 1. Install Whisper

First, install OpenAI's Whisper via Python:

```bash
./setup.sh
```

**Note**: Models are downloaded automatically on first use.

### 2. Run the Application

```bash
# Using default model (base - recommended)
sbt run

# Or specify a different model
sbt "run tiny"    # Fastest for testing
sbt "run small"   # Better accuracy
sbt "run medium"  # High accuracy (slower)
```

### 3. Start Speaking

Once the application starts, speak into your microphone. You'll see transcriptions appear in real-time:

```
============================================================
Real-Time Audio Transcription System
============================================================
Checking for Whisper installation...
✓ Whisper (model: base) is available
Note: Model will be downloaded automatically on first use
✓ Engine initialized successfully
✓ Microphone opened: 16000.0Hz, 16-bit, 1 channel(s)
✓ Microphone capture started
✓ Real-time transcription started (processing 3000ms chunks)
Speak into your microphone...
------------------------------------------------------------

Press Ctrl+C to stop transcription

14:23:45      [en] | Hello, this is a test of the transcription system.
14:23:51      [en] | It works really well and processes audio in real time.
```

## Model Selection Guide

Models are downloaded automatically on first use from OpenAI's servers.

| Model | Size | Speed | Accuracy | Recommendation |
|-------|------|-------|----------|----------------|
| **tiny** | 75 MB | ⚡⚡⚡⚡ | ⭐⭐ | Quick testing |
| **base** | 142 MB | ⚡⚡⚡ | ⭐⭐⭐ | **Best balance** |
| **small** | 466 MB | ⚡⚡ | ⭐⭐⭐⭐ | High accuracy needs |
| **medium** | 1.5 GB | ⚡ | ⭐⭐⭐⭐⭐ | Professional use |
| **large** | 2.9 GB | 🐌 | ⭐⭐⭐⭐⭐ | Best possible quality |

**Recommendation**: Start with `base` - it provides excellent accuracy with reasonable speed for real-time use.

Models are cached in: `~/.cache/whisper/` (Linux/macOS) or `%USERPROFILE%\.cache\whisper\` (Windows)

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
  modelName = "base",                // Change model: tiny, base, small, medium, large
  chunkDurationMs = 3000,            // Process audio every 3 seconds
  silenceThreshold = 0.01f           // Silence detection threshold
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

### "Whisper not installed"
**Solution**: Install Python Whisper
```bash
pip install openai-whisper
# or
pip3 install openai-whisper
```

### "Microphone not supported"
- Check that your microphone is connected and working
- Try listing available audio devices: `arecord -l` (Linux) or check System Preferences (macOS)

### Model download fails
- Ensure you have internet connection (first use only)
- Check disk space (models are cached in ~/.cache/whisper/)
- Try manually downloading: The model will auto-download on first transcription

### Slow transcription
- Try a smaller model: `sbt "run tiny"`
- Increase `chunkDurationMs` for less frequent processing
- Close other resource-intensive applications

### Poor accuracy
- Use a larger model: `sbt "run small"` or `sbt "run medium"`
- Ensure good microphone quality and minimal background noise
- Increase chunk duration for more context

## Technology Stack

- **Language**: Scala 3.5.2
- **JDK**: Java 25
- **Build Tool**: SBT
- **AI Model**: OpenAI Whisper (via Python)
- **Audio**: Java Sound API

## License

This project uses:
- **Whisper** (MIT License) - OpenAI
- **Scala** (Apache 2.0 License)
- **Java** (GPL v2 with Classpath Exception)

## Contributing

Feel free to submit issues or pull requests!

## Future Enhancements

- [ ] Save transcriptions to file
- [ ] Multiple output formats (JSON, SRT subtitles)
- [ ] Voice activity detection (VAD) for better silence handling
- [ ] Support for audio file input
- [ ] Web interface for remote access
- [ ] Speaker diarization (identify different speakers)
