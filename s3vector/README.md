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

## Cost Notes

- OpenSearch Serverless: ~$700/month minimum (OCU-based pricing)
- Bedrock Titan Embeddings: $0.0001 per 1k tokens
- For POC, delete collection after testing to avoid charges

## Cleanup

```bash
aws opensearchserverless delete-collection --id <collection-id>
aws opensearchserverless delete-access-policy --name transactions-access --type data
```
