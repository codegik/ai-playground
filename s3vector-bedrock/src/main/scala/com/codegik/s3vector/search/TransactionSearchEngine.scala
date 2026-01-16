package com.codegik.s3vector.search

import com.codegik.s3vector.domain.Transaction
import com.codegik.s3vector.service.{EmbeddingService, S3VectorSearch}
import software.amazon.awssdk.regions.Region

class TransactionSearchEngine(
  private val embeddingService: EmbeddingService,
  private val s3VectorService: S3VectorSearch
) {

  def initialize(): Either[String, String] = {
    s3VectorService.createBucketIfNotExists()
  }

  private def indexTransaction(transaction: Transaction): Either[String, String] = {
    embeddingService.generateEmbedding(transaction.toSearchableText).flatMap { embedding =>
      s3VectorService.indexTransaction(transaction, embedding)
    }
  }

  def indexTransactions(transactions: List[Transaction]): Unit = {
    transactions.foreach { transaction =>
      println(s"Processing transaction: ${transaction.id} - ${transaction.merchantName}")
      
      indexTransaction(transaction) match {
        case Right(msg) => println(s"  $msg")
        case Left(error) => println(s"  Failed to index: $error")
      }
    }
  }

  def search(query: String, k: Int = 3): Either[String, List[Transaction]] = {
    s3VectorService.search(query, k)
  }

  def searchAndDisplay(query: String, k: Int = 3): Unit = {
    search(query, k) match {
      case Right(results) =>
        println(s"Found ${results.size} results:")
        results.zipWithIndex.foreach { case (transaction, idx) =>
          println(s"  ${idx + 1}. ${transaction.merchantName} - ${transaction.amount}")
          println(s"     ${transaction.description}")
          println(s"     Categories: ${transaction.categories.mkString(", ")}")
        }
      case Left(error) =>
        println(s"Search failed: $error")
    }
  }

  def close(): Unit = {
    embeddingService.close()
    s3VectorService.close()
  }
}

object TransactionSearchEngine {
  def apply(region: Region, bucketName: String, knowledgeBaseId: String): TransactionSearchEngine = {
    val embeddingService = EmbeddingService(region)
    val s3VectorService = S3VectorSearch(region, bucketName, knowledgeBaseId)
    new TransactionSearchEngine(embeddingService, s3VectorService)
  }
}

