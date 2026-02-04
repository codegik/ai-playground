# Usage Guide

## Getting Started

### Option 1: Quick Start (Recommended)

The easiest way to get started:

```bash
./run.sh
```

This script will:
1. Download the base model if not present
2. Compile the project
3. Start the transcription system

### Option 2: Full Setup

For more control over which model to use:

```bash
./setup.sh
```

This interactive script will:
1. Check system requirements (JDK 25, SBT)
2. Let you choose which model to download
3. Download the selected model
4. Compile the project

Then run:
```bash
sbt run
```

### Option 3: Manual Setup

1. Download a model:
```bash
mkdir -p models
wget https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base.bin -O models/ggml-base.bin
```

2. Run with SBT:
```bash
sbt run
```

3. Or specify a custom model:
```bash
sbt "run models/ggml-tiny.bin"
```

## Understanding the Output

When you run the application, you'll see output like this:

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
14:24:02      [es] | También funciona con español.
14:24:08      [fr] | Et avec le français aussi.
```

Each line shows:
- **Timestamp**: When the audio was processed
- **Language code**: Automatically detected language (en, es, fr, etc.)
- **Transcribed text**: What was spoken

## Configuration Options

You can customize the behavior by editing `Main.scala`:

### Chunk Duration

Controls how frequently audio is processed:

```scala
val engine = RealtimeTranscriptionEngine(
  modelPath = modelPath,
  chunkDurationMs = 3000  // Process every 3 seconds
)
```

- **Smaller values (1000-2000ms)**: Faster response, more frequent updates
- **Larger values (3000-5000ms)**: Better accuracy, more context
- **Recommended**: 3000ms for balanced performance

### Silence Threshold

Controls when audio is considered "silence" and skipped:

```scala
val engine = RealtimeTranscriptionEngine(
  modelPath = modelPath,
  chunkDurationMs = 3000,
  silenceThreshold = 0.01f  // 0.0 to 1.0
)
```

- **Lower values (0.001)**: More sensitive, processes quieter audio
- **Higher values (0.05)**: Less sensitive, skips more background noise
- **Recommended**: 0.01 for typical environments

## Advanced Usage

### Using Different Models

Download additional models for different use cases:

```bash
# Fastest (for testing)
wget https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-tiny.bin -O models/ggml-tiny.bin
sbt "run models/ggml-tiny.bin"

# Best accuracy (requires more resources)
wget https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-small.bin -O models/ggml-small.bin
sbt "run models/ggml-small.bin"
```

### Force Specific Language

By default, Whisper auto-detects the language. To force a specific language, modify `WhisperTranscriber.scala`:

```scala
transcriber.transcribe(
  audioChunk, 
  detectLanguage = false,
  language = Some("en")  // Force English
)
```

Supported language codes: en, es, fr, de, it, pt, nl, ru, ar, zh, ja, ko, and 90+ more.

### Save Transcriptions to File

Add this to the callback in `Main.scala`:

```scala
engine.start { result =>
  displayTranscription(result)
  
  // Save to file
  val writer = new java.io.PrintWriter(
    new java.io.FileWriter("transcript.txt", true)
  )
  writer.println(s"${result.language.getOrElse("unknown")}: ${result.text}")
  writer.close()
}
```

## System Requirements

### Minimum Requirements
- **CPU**: Dual-core processor (2.0 GHz+)
- **RAM**: 2 GB (for tiny/base models)
- **Disk**: 200 MB for base model
- **Microphone**: Any USB or built-in microphone

### Recommended Requirements
- **CPU**: Quad-core processor (3.0 GHz+)
- **RAM**: 4 GB (for small/medium models)
- **Disk**: 2 GB for medium model
- **Microphone**: Quality USB microphone for best accuracy

## Performance Benchmarks

Approximate processing times on a modern laptop (Intel i7):

| Model | Model Size | Load Time | Processing Speed | Real-time Factor |
|-------|------------|-----------|------------------|------------------|
| tiny | 75 MB | ~1s | Very Fast | 0.1x (10x faster than real-time) |
| base | 142 MB | ~2s | Fast | 0.2x (5x faster than real-time) |
| small | 466 MB | ~5s | Moderate | 0.5x (2x faster than real-time) |
| medium | 1.5 GB | ~10s | Slow | 1.0x (real-time) |
| large | 2.9 GB | ~20s | Very Slow | 2.0x (half real-time) |

**Real-time Factor**: How fast the model processes compared to audio duration. Lower is better for real-time use.

## Tips for Best Results

### Audio Quality
1. **Use a quality microphone** - USB microphones generally work better than built-in ones
2. **Minimize background noise** - Close windows, turn off fans
3. **Speak clearly** - Normal speaking pace, not too fast or slow
4. **Optimal distance** - 15-30cm from microphone

### Model Selection
1. **For real-time use**: Use `tiny` or `base` models
2. **For accuracy**: Use `small` or `medium` models
3. **For multiple languages**: Larger models handle languages better
4. **For English only**: All models perform well, even tiny

### Performance Optimization
1. **Close other applications** to free up CPU
2. **Increase chunk duration** if processing is too slow
3. **Use smaller model** if experiencing lag
4. **Check system resources** with `top` or `htop`

## Troubleshooting

### No audio being captured
```bash
# List audio devices (Linux)
arecord -l

# Test microphone
arecord -d 5 test.wav
aplay test.wav
```

### Model download fails
- Check internet connection
- Try manual download from: https://huggingface.co/ggerganov/whisper.cpp/tree/main
- Use alternative mirror if HuggingFace is slow

### Out of memory errors
- Use a smaller model (tiny or base)
- Close other applications
- Increase JVM heap size: `sbt -J-Xmx4G run`

### Poor transcription quality
- Try a larger model
- Reduce background noise
- Speak more clearly
- Check microphone positioning

### Lag or slow processing
- Use a smaller model (tiny)
- Increase chunk duration to 5000ms
- Close resource-intensive applications
- Check CPU usage

## Getting Help

If you encounter issues:

1. **Check the logs** - Look for error messages in the console output
2. **Verify requirements** - Ensure JDK 25 and proper model file
3. **Test microphone** - Verify audio input works with other applications
4. **Try different model** - Some models may work better for your hardware

## Examples

### Example 1: Quick Testing
```bash
# Download tiny model for quick testing
mkdir -p models
wget https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-tiny.bin -O models/ggml-tiny.bin
sbt "run models/ggml-tiny.bin"
```

### Example 2: Production Use
```bash
# Download medium model for high accuracy
mkdir -p models
wget https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-medium.bin -O models/ggml-medium.bin
sbt "run models/ggml-medium.bin"
```

### Example 3: Multiple Languages
```bash
# Use base model with auto language detection (default)
./run.sh
# Speak in any language - it will auto-detect!
```
