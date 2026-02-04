#!/usr/bin/env python3
"""
Faster-Whisper Server for Real-Time Transcription
Uses faster-whisper which is 4x faster than original whisper and keeps model in memory
"""

import sys
import json
import warnings
warnings.filterwarnings("ignore")

try:
    from faster_whisper import WhisperModel
except ImportError:
    print("ERROR: faster-whisper not installed", file=sys.stderr)
    print("Install with: pip3 install faster-whisper", file=sys.stderr)
    sys.exit(1)

def main():
    # Get model name from argument or use default
    model_name = sys.argv[1] if len(sys.argv) > 1 else "base"

    print(f"Loading faster-whisper model: {model_name}...", file=sys.stderr, flush=True)
    print("This will be MUCH more accurate than Vosk!", file=sys.stderr, flush=True)

    try:
        # Load model ONCE with CPU settings for best compatibility
        # device="cpu" ensures it works everywhere
        # compute_type="int8" for faster processing on CPU
        model = WhisperModel(model_name, device="cpu", compute_type="int8")

        print(f"✓ faster-whisper model '{model_name}' loaded successfully!", file=sys.stderr, flush=True)
        print("✓ Ready for Google-like accuracy transcription!", file=sys.stderr, flush=True)
        print("READY", flush=True)  # Signal to Scala that we're ready

        # Process audio files from stdin
        while True:
            line = sys.stdin.readline()
            if not line:
                break

            audio_path = line.strip()
            if not audio_path or audio_path == "QUIT":
                break

            try:
                # Transcribe with faster-whisper
                # beam_size=1 for faster real-time processing
                # vad_filter=True to filter silence and improve accuracy
                segments, info = model.transcribe(
                    audio_path,
                    beam_size=1,
                    vad_filter=True,
                    vad_parameters=dict(min_silence_duration_ms=500)
                )

                # Collect all segments
                text_parts = []
                for segment in segments:
                    text_parts.append(segment.text)

                # Send result as JSON
                output = {
                    "text": " ".join(text_parts).strip(),
                    "language": info.language
                }
                print(json.dumps(output), flush=True)

            except Exception as e:
                # Send error as JSON
                output = {
                    "text": "",
                    "language": "unknown",
                    "error": str(e)
                }
                print(json.dumps(output), flush=True)

    except Exception as e:
        print(f"ERROR: {e}", file=sys.stderr, flush=True)
        sys.exit(1)

if __name__ == "__main__":
    main()
