#!/bin/bash

# Setup script for Real-Time Audio Transcription System

echo "=================================================="
echo "Real-Time Audio Transcription - Setup"
echo "=================================================="
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

# Create models directory
echo ""
echo "Creating models directory..."
mkdir -p models
echo "✓ Models directory created"

# Download model
echo ""
echo "Available Whisper models:"
echo "  1. tiny   (~75 MB)  - Fastest, less accurate"
echo "  2. base   (~142 MB) - Good balance (RECOMMENDED)"
echo "  3. small  (~466 MB) - Better accuracy"
echo "  4. medium (~1.5 GB) - High accuracy"
echo "  5. large  (~2.9 GB) - Best accuracy"
echo ""
echo -n "Which model do you want to download? [1-5] (default: 2): "
read choice

case $choice in
    1)
        MODEL="ggml-tiny.bin"
        ;;
    3)
        MODEL="ggml-small.bin"
        ;;
    4)
        MODEL="ggml-medium.bin"
        ;;
    5)
        MODEL="ggml-large-v3.bin"
        ;;
    *)
        MODEL="ggml-base.bin"
        ;;
esac

MODEL_PATH="models/$MODEL"

if [ -f "$MODEL_PATH" ]; then
    echo "✓ Model already exists: $MODEL_PATH"
else
    echo ""
    echo "Downloading $MODEL..."
    wget "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/$MODEL" -O "$MODEL_PATH"

    if [ $? -eq 0 ]; then
        echo "✓ Model downloaded successfully"
    else
        echo "❌ Failed to download model"
        echo "You can manually download from: https://huggingface.co/ggerganov/whisper.cpp/tree/main"
        exit 1
    fi
fi

# Compile the project
echo ""
echo "Compiling project..."
sbt compile

if [ $? -eq 0 ]; then
    echo ""
    echo "=================================================="
    echo "✓ Setup completed successfully!"
    echo "=================================================="
    echo ""
    echo "To run the application:"
    echo "  sbt \"run $MODEL_PATH\""
    echo ""
    echo "Or simply:"
    echo "  sbt run"
    echo ""
else
    echo "❌ Compilation failed"
    exit 1
fi
