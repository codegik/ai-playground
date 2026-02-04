#!/bin/bash

# Quick setup script for Vosk real-time transcription

echo "============================================================"
echo "Real-Time Transcription Setup - Using Vosk (FAST!)"
echo "============================================================"
echo ""

# Create directories
mkdir -p lib
mkdir -p models

# Download Vosk Java library
if [ ! -f "lib/vosk-0.3.45.jar" ]; then
    echo "Downloading Vosk Java library..."
    cd lib
    wget https://github.com/alphacep/vosk-api/releases/download/v0.3.45/vosk-0.3.45.jar
    cd ..
    echo "✓ Vosk library downloaded"
else
    echo "✓ Vosk library already exists"
fi

# Download native libraries based on OS
echo ""
echo "Downloading native libraries for your system..."
cd lib

if [[ "$OSTYPE" == "linux-gnu"* ]]; then
    if [ ! -f "libvosk.so" ]; then
        echo "Detected Linux - downloading native library..."
        wget https://github.com/alphacep/vosk-api/releases/download/v0.3.45/vosk-linux-x86_64-0.3.45.zip
        unzip -o vosk-linux-x86_64-0.3.45.zip
        mv vosk-linux-x86_64-0.3.45/libvosk.so .
        rm -rf vosk-linux-x86_64-0.3.45 vosk-linux-x86_64-0.3.45.zip
        echo "✓ Linux native library installed"
    else
        echo "✓ Linux native library already exists"
    fi
elif [[ "$OSTYPE" == "darwin"* ]]; then
    if [ ! -f "libvosk.dylib" ]; then
        echo "Detected macOS - downloading native library..."
        wget https://github.com/alphacep/vosk-api/releases/download/v0.3.45/vosk-osx-0.3.45.zip
        unzip -o vosk-osx-0.3.45.zip
        mv vosk-osx-0.3.45/libvosk.dylib .
        rm -rf vosk-osx-0.3.45 vosk-osx-0.3.45.zip
        echo "✓ macOS native library installed"
    else
        echo "✓ macOS native library already exists"
    fi
else
    echo "⚠ Unsupported OS. Please download manually from:"
    echo "   https://github.com/alphacep/vosk-api/releases"
fi

cd ..

# Download Vosk model
echo ""
echo "Available models:"
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
        echo "Skipping model download"
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
        unzip "$MODEL_FILE"
        rm "$MODEL_FILE"
        cd ..
        echo "✓ Model downloaded: models/$MODEL_DIR"
    else
        echo "✓ Model already exists: models/$MODEL_DIR"
    fi
fi

# Set library path
export LD_LIBRARY_PATH="$PWD/lib:$LD_LIBRARY_PATH"
export DYLD_LIBRARY_PATH="$PWD/lib:$DYLD_LIBRARY_PATH"

echo ""
echo "Compiling project..."
sbt compile

if [ $? -eq 0 ]; then
    echo ""
    echo "============================================================"
    echo "✓ Setup completed successfully!"
    echo "============================================================"
    echo ""
    echo "To run:"
    echo "  export LD_LIBRARY_PATH=$PWD/lib:\$LD_LIBRARY_PATH"
    echo "  sbt run"
    echo ""
    echo "Or use the run script:"
    echo "  ./run.sh"
    echo ""
    echo "Vosk is MUCH FASTER than Whisper for real-time transcription!"
    echo ""
else
    echo "❌ Compilation failed"
    exit 1
fi
