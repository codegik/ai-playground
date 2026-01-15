#!/bin/bash

echo "Stopping and removing containers..."
podman stop opensearch ollama 2>/dev/null || true
podman rm opensearch ollama 2>/dev/null || true

echo "Services stopped successfully!"
