#!/bin/bash

# Quick start script for Vosk real-time transcription

# Set library path for native libraries
export LD_LIBRARY_PATH="$PWD/lib:$LD_LIBRARY_PATH"
export DYLD_LIBRARY_PATH="$PWD/lib:$DYLD_LIBRARY_PATH"

echo "Starting real-time transcription with Vosk..."
sbt run
