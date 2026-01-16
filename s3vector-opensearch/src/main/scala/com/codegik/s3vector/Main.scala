package com.codegik.s3vector

import com.codegik.s3vector.domain.Transaction
import com.codegik.s3vector.search.TransactionSearchEngine

import java.time.LocalDateTime

object Main {
  def main(args: Array[String]): Unit = {
    val openSearchEndpoint = sys.env.getOrElse("OPENSEARCH_ENDPOINT", "http://localhost:9200")
    val searchEngine = TransactionSearchEngine(openSearchEndpoint)

    println("Creating OpenSearch index...")
    searchEngine.initialize() match {
      case Right(response) => println(s"Index created: $response")
      case Left(error) => println(s"Index creation (or already exists): $error")
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

    println("\nIndexing transactions...")
    searchEngine.indexTransactions(transactions)

    Thread.sleep(2000)

    println("\n\nSearching for 'morning coffee'...")
    searchEngine.searchAndDisplay("morning coffee")

    println("\n\nSearching for 'electronics shopping'...")
    searchEngine.searchAndDisplay("electronics shopping")

    println("\n\nSearching for 'gas station fuel'...")
    searchEngine.searchAndDisplay("gas station fuel")

    searchEngine.close()
  }
}
