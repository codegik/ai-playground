#!/bin/bash

set -e

echo "Starting OpenSearch..."
docker run -d --name opensearch \
  -p 9200:9200 -p 9600:9600 \
  -e "discovery.type=single-node" \
  -e "DISABLE_SECURITY_PLUGIN=true" \
  -e "OPENSEARCH_JAVA_OPTS=-Xms512m -Xmx512m" \
  opensearchproject/opensearch:latest

echo "Starting Ollama..."
docker run -d --name ollama \
  -p 11434:11434 \
  -v ollama-data:/root/.ollama \
  ollama/ollama:latest

echo "Waiting for Ollama to be ready..."
max_attempts=30
attempt=0
while ! curl -s http://localhost:11434/api/tags >/dev/null; do
  attempt=$((attempt + 1))
  if [ $attempt -ge $max_attempts ]; then
    echo "Ollama failed to start"
    exit 1
  fi
  sleep 1
done

echo "Pulling nomic-embed-text model..."
docker exec ollama ollama pull nomic-embed-text

echo "Waiting for OpenSearch to be ready..."
attempt=0
while ! curl -s http://localhost:9200/_cluster/health >/dev/null; do
  attempt=$((attempt + 1))
  if [ $attempt -ge $max_attempts ]; then
    echo "OpenSearch failed to start"
    exit 1
  fi
  sleep 1
done

echo "All services started successfully!"
echo "OpenSearch: http://localhost:9200"
echo "Ollama: http://localhost:11434"
