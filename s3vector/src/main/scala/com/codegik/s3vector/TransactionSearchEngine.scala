package com.codegik.s3vector

class TransactionSearchEngine(
  private val embeddingService: EmbeddingService,
  private val openSearchService: OpenSearchService
) {

  def initialize(): Either[String, String] = {
    openSearchService.createIndex()
  }

  def indexTransaction(transaction: Transaction): Either[String, String] = {
    embeddingService.generateEmbedding(transaction.toSearchableText).flatMap { embedding =>
      openSearchService.indexTransaction(transaction, embedding)
    }
  }

  def indexTransactions(transactions: List[Transaction]): Unit = {
    transactions.foreach { transaction =>
      println(s"Processing transaction: ${transaction.id} - ${transaction.merchantName}")
      
      indexTransaction(transaction) match {
        case Right(_) => println(s"  Indexed successfully")
        case Left(error) => println(s"  Failed to index: $error")
      }
    }
  }

  def search(query: String, k: Int = 3): Either[String, List[Transaction]] = {
    embeddingService.generateEmbedding(query).flatMap { queryEmbedding =>
      openSearchService.search(queryEmbedding, k)
    }
  }

  def searchAndDisplay(query: String, k: Int = 3): Unit = {
    search(query, k) match {
      case Right(results) =>
        println(s"Found ${results.size} results:")
        results.zipWithIndex.foreach { case (transaction, idx) =>
          println(s"  ${idx + 1}. ${transaction.merchantName} - $${transaction.amount}")
          println(s"     ${transaction.description}")
          println(s"     Categories: ${transaction.categories.mkString(", ")}")
        }
      case Left(error) =>
        println(s"Search failed: $error")
    }
  }

  def close(): Unit = {
    openSearchService.close()
  }
}

object TransactionSearchEngine {
  def apply(openSearchEndpoint: String): TransactionSearchEngine = {
    val embeddingService = EmbeddingService()
    val openSearchService = OpenSearchService(openSearchEndpoint)
    new TransactionSearchEngine(embeddingService, openSearchService)
  }
}

