#!/bin/bash

# Quick start script - checks for Whisper installation and runs the application

echo "Checking for Whisper installation..."

if ! python3 -c "import whisper" 2>/dev/null; then
    echo "Whisper not installed. Installing..."
    pip3 install openai-whisper

    if [ $? -ne 0 ]; then
        echo "Failed to install Whisper"
        echo "Please install manually: pip3 install openai-whisper"
        exit 1
    fi
    echo "✓ Whisper installed"
fi

echo "Starting real-time transcription..."
sbt run
