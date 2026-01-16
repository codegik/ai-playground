# S3 Vector Search with AWS Bedrock POC

Credit card transaction search using AWS Bedrock embeddings and S3 with Amazon Bedrock Knowledge Bases.

## Architecture

- **Data**: Credit card transactions with categorization stored in S3
- **Embeddings**: AWS Bedrock Titan Embeddings V2 (1024 dimensions)
- **Vector DB**: Amazon Bedrock Knowledge Bases with S3 data source
- **Language**: Scala 3.3.4

## Prerequisites

- SBT (Scala Build Tool)
- Java 25
- AWS Account with appropriate permissions
- AWS CLI configured with credentials

## AWS Setup

Ensure your AWS credentials are configured:

```bash
export AWS_ACCESS_KEY_ID=your_access_key
export AWS_SECRET_ACCESS_KEY=your_secret_key
export AWS_REGION=us-east-1
```

## Running the Application

```bash
sbt run
```

## What It Does

1. Creates S3 bucket (if not exists) for storing transaction data
2. Generates embeddings for each transaction using AWS Bedrock Titan model
3. Stores transactions as JSON documents in S3 with embeddings
4. Indexes 4 transactions:
   - Starbucks coffee purchase
   - Gas station fuel
   - Amazon electronics
   - McDonald's fast food
5. Waits for Knowledge Base to sync
6. Searches using vector similarity via Bedrock Knowledge Bases:
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

## Project Structure

```
src/main/scala/com/codegik/s3vector/
├── Main.scala                    - Entry point
├── Transaction.scala             - Data model
├── EmbeddingService.scala        - AWS Bedrock client
├── OpenSearchService.scala       - S3 + Knowledge Base client
└── TransactionSearchEngine.scala - Search orchestration
```

## Cleanup

To avoid ongoing charges:

1. Delete Knowledge Base in AWS Console
2. Delete S3 bucket:
   ```bash
   aws s3 rb s3://transaction-vector-search --force
   ```
3. Delete OpenSearch Serverless collection (if created)
