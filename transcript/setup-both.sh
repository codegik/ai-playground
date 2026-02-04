#!/bin/bash

# Complete setup script for Real-Time Audio Transcription System
# Sets up BOTH Vosk and faster-whisper engines

echo "============================================================"
echo "Real-Time Transcription Setup - BOTH Engines"
echo "============================================================"
echo ""
echo "This script will set up:"
echo "  1. Vosk (fast, lower latency)"
echo "  2. faster-whisper (accurate, Google-like)"
echo ""

# Check for JDK 25
echo "Checking Java version..."
JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)

if [ "$JAVA_VERSION" != "25" ]; then
    echo "❌ Error: JDK 25 required, but found JDK $JAVA_VERSION"
    echo "Please install JDK 25 and set it as default"
    exit 1
else
    echo "✓ JDK 25 detected"
fi

# Check for SBT
echo "Checking SBT..."
if ! command -v sbt &> /dev/null; then
    echo "❌ Error: SBT not found"
    echo "Please install SBT: https://www.scala-sbt.org/download.html"
    exit 1
else
    echo "✓ SBT detected"
fi

# Check for Python 3
echo "Checking Python 3..."
if ! command -v python3 &> /dev/null; then
    echo "❌ Error: Python 3 not found"
    echo "Please install Python 3: https://www.python.org/downloads/"
    exit 1
else
    PYTHON_VERSION=$(python3 --version)
    echo "✓ $PYTHON_VERSION detected"
fi

echo ""
echo "============================================================"
echo "PART 1: Setting up faster-whisper (Google-like accuracy)"
echo "============================================================"
echo ""

# Install faster-whisper
echo "Checking for faster-whisper installation..."
if python3 -c "import faster_whisper" 2>/dev/null; then
    echo "✓ faster-whisper is already installed"
else
    echo "Installing faster-whisper..."
    pip3 install faster-whisper

    if [ $? -eq 0 ]; then
        echo "✓ faster-whisper installed successfully"
    else
        echo "⚠ Failed to install faster-whisper"
        echo "You can install manually later: pip3 install faster-whisper"
    fi
fi

echo ""
echo "============================================================"
echo "PART 2: Setting up Vosk (fast, low latency)"
echo "============================================================"
echo ""

# Create models directory
mkdir -p models
echo "✓ Models directory created"

# Download Vosk model
echo ""
echo "Available Vosk models:"
echo "  1. Small English (~40 MB) - RECOMMENDED for real-time"
echo "  2. Large English (~1.8 GB) - Better accuracy"
echo "  3. Skip (download manually later)"
echo ""
echo -n "Choose [1-3] (default: 1): "
read choice

case $choice in
    2)
        MODEL_FILE="vosk-model-en-us-0.22.zip"
        MODEL_DIR="vosk-model-en-us-0.22"
        MODEL_URL="https://alphacephei.com/vosk/models/$MODEL_FILE"
        ;;
    3)
        echo "Skipping Vosk model download"
        MODEL_FILE=""
        ;;
    *)
        MODEL_FILE="vosk-model-small-en-us-0.15.zip"
        MODEL_DIR="vosk-model-small-en-us-0.15"
        MODEL_URL="https://alphacephei.com/vosk/models/$MODEL_FILE"
        ;;
esac

if [ -n "$MODEL_FILE" ]; then
    if [ ! -d "models/$MODEL_DIR" ]; then
        echo ""
        echo "Downloading $MODEL_FILE..."
        cd models
        wget "$MODEL_URL"

        if [ $? -eq 0 ]; then
            unzip "$MODEL_FILE"
            rm "$MODEL_FILE"
            cd ..
            echo "✓ Vosk model downloaded: models/$MODEL_DIR"
        else
            echo "⚠ Failed to download Vosk model"
            echo "You can download manually from: https://alphacephei.com/vosk/models"
            cd ..
        fi
    else
        echo "✓ Vosk model already exists: models/$MODEL_DIR"
    fi
fi

echo ""
echo "============================================================"
echo "PART 3: Compiling the project"
echo "============================================================"
echo ""

sbt compile

if [ $? -eq 0 ]; then
    echo ""
    echo "============================================================"
    echo "✓ Setup completed successfully!"
    echo "============================================================"
    echo ""
    echo "Both engines are now available!"
    echo ""
    echo "USAGE:"
    echo "------"
    echo ""
    echo "Use faster-whisper (Google-like accuracy, slower):"
    echo "  sbt \"run whisper\""
    echo "  sbt \"run whisper tiny\"    # Faster but less accurate"
    echo "  sbt \"run whisper base\"    # Balanced (default)"
    echo "  sbt \"run whisper small\"   # Better accuracy"
    echo "  sbt \"run whisper medium\"  # Best accuracy"
    echo ""
    echo "Use Vosk (fast, lower latency):"
    echo "  sbt \"run vosk\""
    echo ""
    echo "Auto-detect (uses Vosk if available, otherwise faster-whisper):"
    echo "  sbt run"
    echo ""
    echo "COMPARISON:"
    echo "-----------"
    echo "Vosk:            Fast (1-2s latency), good accuracy"
    echo "faster-whisper:  Slower (2-4s latency), Google-like accuracy"
    echo ""
    echo "RECOMMENDATION:"
    echo "---------------"
    echo "- Use Vosk for speed and low latency"
    echo "- Use faster-whisper for maximum accuracy"
    echo ""
else
    echo "❌ Compilation failed"
    exit 1
fi
