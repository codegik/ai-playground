# S3 Vector Search POC

Credit card transaction search using local OpenSearch and Ollama embeddings.

## Architecture

- **Data**: Credit card transactions with categorization
- **Embeddings**: Ollama nomic-embed-text (768 dimensions)
- **Vector DB**: OpenSearch with k-NN search
- **Language**: Scala 3.3.4

## Prerequisites

- Podman (or Docker)
- SBT (Scala Build Tool)
- Java 11+

## Local Setup

### 1. Start Services

```bash
./start.sh
```

This starts:
- OpenSearch on port 9200
- Ollama on port 11434
- Pulls nomic-embed-text model

### 2. Verify Services

```bash
curl http://localhost:9200/_cluster/health
curl http://localhost:11434/api/tags
```

### 3. Compile and Run

```bash
sbt compile
sbt run
```

## What It Does

1. Creates OpenSearch index with vector field (768 dimensions)
2. Generates embeddings for each transaction using Ollama
3. Indexes 4 transactions:
   - Starbucks coffee purchase
   - Gas station fuel
   - Amazon electronics
   - McDonald's fast food
4. Searches using vector similarity:
   - "morning coffee" → finds Starbucks
   - "electronics shopping" → finds Amazon
   - "gas station fuel" → finds Shell

## Transaction Structure

```scala
case class Transaction(
  id: String,
  amount: Double,
  merchantName: String,
  timestamp: LocalDateTime,
  description: String,
  categories: List[String]
)
```

## Stopping Services

```bash
./stop.sh
```

## Troubleshooting

### Certificate Errors (Ollama)

If you get certificate errors when pulling models:

```bash
podman machine ssh
sudo timedatectl set-ntp true
exit
podman restart ollama
podman exec ollama ollama pull nomic-embed-text
```

### OpenSearch Logs

```bash
podman logs opensearch
```

## Project Structure

```
src/main/scala/com/codegik/s3vector/
├── Main.scala                    - Entry point
├── Transaction.scala             - Data model
├── EmbeddingService.scala        - Ollama client
├── OpenSearchService.scala       - Vector DB client
└── TransactionSearchEngine.scala - Search orchestration
```
