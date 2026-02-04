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

 Check for Python 3
echo "Checking Python 3..."
if ! command -v python3 &> /dev/null; then
    echo "❌ Error: Python 3 not found"
    echo "Please install Python 3: https://www.python.org/downloads/"
    exit 1
else
    PYTHON_VERSION=$(python3 --version)
    echo "✓ $PYTHON_VERSION detected"
fi

# Check/Install faster-whisper (much faster and more accurate than Vosk)
echo ""
echo "Checking for faster-whisper installation..."
if python3 -c "import faster_whisper" 2>/dev/null; then
    echo "✓ faster-whisper is already installed"
else
    echo "faster-whisper not found. Installing..."
    echo ""
    echo "This will install faster-whisper (optimized for real-time transcription)"
    echo ""
    echo "Choose installation method:"
    echo "  1. pip3 install (recommended)"
    echo "  2. Skip (install manually later)"
    echo ""
    echo -n "Choice [1-2] (default: 1): "
    read choice

    if [ "$choice" != "2" ]; then
        pip3 install faster-whisper

        if [ $? -eq 0 ]; then
            echo "✓ faster-whisper installed successfully"
        else
            echo "❌ Failed to install faster-whisper"
            echo "Try manually: pip3 install faster-whisper"
            exit 1
        fi
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
    echo "This system uses faster-whisper for Google-like accuracy!"
    echo ""
    echo "Available models (download automatically on first use):"
    echo "  - tiny   (~75 MB)  - Fast but less accurate"
    echo "  - base   (~142 MB) - Good balance (RECOMMENDED)"
    echo "  - small  (~466 MB) - Better accuracy"
    echo "  - medium (~1.5 GB) - High accuracy (like Google Translate)"
    echo ""
    echo "To run the application:"
    echo "  sbt run                  # Auto-detects any Vosk model OR uses faster-whisper"
    echo ""
    echo "Note: First run will download the model (may take a few minutes)"
    echo ""
else
    echo "❌ Compilation failed"
    exit 1
fi
