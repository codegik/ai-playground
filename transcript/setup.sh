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

# Check/Install Whisper
echo ""
echo "Checking for Whisper installation..."
if python3 -c "import whisper" 2>/dev/null; then
    echo "✓ Whisper is already installed"
else
    echo "Whisper not found. Installing..."
    echo ""
    echo "Choose installation method:"
    echo "  1. pip3 install (recommended)"
    echo "  2. Skip (install manually later)"
    echo ""
    echo -n "Choice [1-2] (default: 1): "
    read choice

    if [ "$choice" != "2" ]; then
        pip3 install openai-whisper

        if [ $? -eq 0 ]; then
            echo "✓ Whisper installed successfully"
        else
            echo "❌ Failed to install Whisper"
            echo "Try manually: pip3 install openai-whisper"
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
    echo "Available models:"
    echo "  - tiny   (~75 MB)  - Fastest"
    echo "  - base   (~142 MB) - Recommended"
    echo "  - small  (~466 MB) - Better accuracy"
    echo "  - medium (~1.5 GB) - High accuracy"
    echo "  - large  (~2.9 GB) - Best accuracy"
    echo ""
    echo "Models download automatically on first use."
    echo ""
    echo "To run the application:"
    echo "  sbt run                  # Uses 'base' model"
    echo "  sbt \"run tiny\"         # Uses 'tiny' model (fastest)"
    echo "  sbt \"run small\"        # Uses 'small' model (better accuracy)"
    echo ""
else
    echo "❌ Compilation failed"
    exit 1
fi
