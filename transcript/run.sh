#!/bin/bash

# Quick start script - downloads model and runs the application

MODEL_PATH="models/ggml-base.bin"

if [ ! -f "$MODEL_PATH" ]; then
    echo "Model not found. Downloading..."
    mkdir -p models
    wget "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base.bin" -O "$MODEL_PATH"

    if [ $? -ne 0 ]; then
        echo "Failed to download model"
        exit 1
    fi
fi

echo "Starting real-time transcription..."
sbt "run $MODEL_PATH"
