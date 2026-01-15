# S3 Vector Search POC

Credit card transaction search using AWS OpenSearch Serverless with vector engine.

## Architecture

- **Data**: Credit card transactions with categorization
- **Embeddings**: AWS Bedrock Titan Embed Text v1
- **Vector DB**: AWS OpenSearch Serverless with k-NN search
- **Language**: Scala 3.3.4

## Prerequisites

1. AWS Account with:
   - AWS Bedrock access (Titan Embed Text v1 model enabled)
   - OpenSearch Serverless permissions

2. AWS CLI configured with credentials:
```bash
aws configure
```

## AWS OpenSearch Serverless Setup

### 1. Create Collection

```bash
aws opensearchserverless create-collection \
  --name transactions \
  --type VECTORSEARCH
```

### 2. Create Data Access Policy

Save as `data-access-policy.json`:
```json
[{
  "Rules": [{
    "ResourceType": "index",
    "Resource": ["index/transactions/*"],
    "Permission": ["aoss:*"]
  }],
  "Principal": ["arn:aws:iam::<YOUR_ACCOUNT_ID>:user/<YOUR_USER>"]
}]
```

Apply policy:
```bash
aws opensearchserverless create-access-policy \
  --name transactions-access \
  --type data \
  --policy file://data-access-policy.json
```

### 3. Get Collection Endpoint

```bash
aws opensearchserverless batch-get-collection --names transactions
```

Note the `collectionEndpoint` from output.

## Run POC

### 1. Set environment variable

```bash
export OPENSEARCH_ENDPOINT=https://xxxxx.us-east-1.aoss.amazonaws.com
```

### 2. Compile and run

```bash
sbt run
```

## What It Does

1. Creates OpenSearch index with vector field (1536 dimensions)
2. Indexes 4 transactions:
   - Starbucks coffee purchase
   - Gas station fuel
   - Amazon electronics
   - McDonald's fast food
3. Searches using vector similarity:
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
