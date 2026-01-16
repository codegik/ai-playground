package com.codegik.s3vector

import com.codegik.s3vector.domain.Transaction
import com.codegik.s3vector.search.TransactionSearchEngine
import software.amazon.awssdk.regions.Region

import java.time.LocalDateTime

object Main {
  def main(args: Array[String]): Unit = {
    // Configuration from environment variables or defaults
    val region = Region.of(sys.env.getOrElse("AWS_REGION", "us-east-1"))
    val bucketName = sys.env.getOrElse("S3_BUCKET_NAME", "transaction-vector-search")
    val knowledgeBaseId = sys.env.getOrElse("KNOWLEDGE_BASE_ID", "YOUR_KNOWLEDGE_BASE_ID")

    println(s"Initializing with:")
    println(s"  Region: ${region.id()}")
    println(s"  S3 Bucket: $bucketName")
    println(s"  Knowledge Base ID: $knowledgeBaseId")

    val searchEngine = TransactionSearchEngine(region, bucketName, knowledgeBaseId)

    println("\nCreating S3 bucket if not exists...")
    searchEngine.initialize() match {
      case Right(response) => println(s"  $response")
      case Left(error) => println(s"  Initialization: $error")
    }

    val transactions = List(
      Transaction(
        id = "txn_001",
        amount = 45.99,
        merchantName = "Starbucks",
        timestamp = LocalDateTime.now().minusDays(1),
        description = "Coffee and breakfast sandwich",
        categories = List("food", "breakfast", "coffee")
      ),
      Transaction(
        id = "txn_002",
        amount = 120.50,
        merchantName = "Shell Gas Station",
        timestamp = LocalDateTime.now().minusDays(2),
        description = "Fuel purchase",
        categories = List("transportation", "fuel")
      ),
      Transaction(
        id = "txn_003",
        amount = 89.99,
        merchantName = "Amazon",
        timestamp = LocalDateTime.now().minusDays(3),
        description = "Electronics purchase - wireless mouse",
        categories = List("shopping", "electronics", "office")
      ),
      Transaction(
        id = "txn_004",
        amount = 25.00,
        merchantName = "McDonald's",
        timestamp = LocalDateTime.now().minusDays(4),
        description = "Fast food lunch",
        categories = List("food", "lunch", "fast-food")
      )
    )

    println("\nIndexing transactions to S3...")
    searchEngine.indexTransactions(transactions)

    // Wait for Knowledge Base to sync (in production, you'd check sync status)
    println("\nWaiting for Knowledge Base to sync...")
    Thread.sleep(5000)

    println("\n\nSearching for 'morning coffee'...")
    searchEngine.searchAndDisplay("morning coffee")

    println("\n\nSearching for 'electronics shopping'...")
    searchEngine.searchAndDisplay("electronics shopping")

    println("\n\nSearching for 'gas station fuel'...")
    searchEngine.searchAndDisplay("gas station fuel")

    searchEngine.close()
  }
}
