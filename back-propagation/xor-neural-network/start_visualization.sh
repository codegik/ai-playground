#!/bin/bash

cleanup() {
    echo "Shutting down server..."
    if [ ! -z "$SERVER_PID" ]; then
        kill $SERVER_PID 2>/dev/null
    fi

    echo "Cleaning up frames..."
    rm -rf frames
    echo "Cleanup complete. Goodbye!"
    exit 0
}

trap cleanup SIGINT SIGTERM

echo "Starting Backpropagation Visualization..."
echo ""

echo "Step 1: Generating animation frames..."
python3 video_generator.py
echo ""

echo "Step 2: Starting web server on http://localhost:8000..."
python3 -m http.server 8000 &
SERVER_PID=$!

echo "Server running with PID: $SERVER_PID"
echo ""
echo "Opening visualization in browser..."
sleep 1
open http://localhost:8000/video.html

echo ""
echo "================================"
echo "Visualization is ready!"
echo "Press Ctrl+C to stop the server and clean up"
echo "================================"
echo ""

wait $SERVER_PID
