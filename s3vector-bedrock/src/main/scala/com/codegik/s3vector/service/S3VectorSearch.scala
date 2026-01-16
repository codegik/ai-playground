package com.codegik.s3vector.service

import cats.implicits.*
import com.codegik.s3vector.S3VectorService
import com.codegik.s3vector.domain.Transaction
import io.circe.*
import io.circe.parser.*
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.{CreateBucketRequest, HeadBucketRequest, PutObjectRequest}

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import scala.jdk.CollectionConverters.*
import scala.util.Try

class S3VectorSearch(region: Region, bucketName: String, knowledgeBaseId: String) {
  private val s3Client: S3Client = S3Client.builder()
    .region(region)
    .credentialsProvider(DefaultCredentialsProvider.create())
    .build()

  def createBucketIfNotExists(): Either[String, String] = {
    Try {
      try {
        s3Client.headBucket(HeadBucketRequest.builder().bucket(bucketName).build())
        s"Bucket $bucketName already exists"
      } catch {
        case _: Exception =>
          s3Client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build())
          s"Bucket $bucketName created successfully"
      }
    }.toEither.left.map(err => s"Failed to create/check bucket: ${err.getMessage}")
  }

  def indexTransaction(transaction: Transaction, embedding: Array[Float]): Either[String, String] = {
    Try {
      // Create a JSON document with metadata and embedding
      val doc = Json.obj(
        "transaction_id" -> Json.fromString(transaction.id),
        "amount" -> Json.fromDoubleOrNull(transaction.amount),
        "merchant_name" -> Json.fromString(transaction.merchantName),
        "timestamp" -> Json.fromString(transaction.timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)),
        "description" -> Json.fromString(transaction.description),
        "categories" -> Json.fromValues(transaction.categories.map(Json.fromString)),
        "text" -> Json.fromString(transaction.toSearchableText),
        "embedding" -> Json.fromValues(embedding.map(f => Json.fromFloatOrNull(f)))
      )

      val key = s"transactions/${transaction.id}.json"
      
      val putRequest = PutObjectRequest.builder()
        .bucket(bucketName)
        .key(key)
        .contentType("application/json")
        .build()

      s3Client.putObject(putRequest, RequestBody.fromString(doc.noSpaces))
      s"Transaction ${transaction.id} indexed to S3"
    }.toEither.left.map(err => s"Failed to index transaction: ${err.getMessage}")
  }

  def search(queryText: String, k: Int = 5): Either[String, List[Transaction]] = {
    // TODO: Implement proper vector search using Bedrock Agent Runtime when SDK is available
    // For now, return a placeholder implementation
    Left(s"Search functionality requires Bedrock Agent Runtime SDK which is not yet available. Knowledge Base ID: $knowledgeBaseId")
  }

  private def parseTransactionFromJson(jsonString: String): Either[String, Transaction] = {
    parse(jsonString).flatMap { json =>
      val cursor = json.hcursor
      for {
        id <- cursor.downField("transaction_id").as[String]
        amount <- cursor.downField("amount").as[Double]
        merchant <- cursor.downField("merchant_name").as[String]
        timestamp <- cursor.downField("timestamp").as[String]
        description <- cursor.downField("description").as[String]
        categories <- cursor.downField("categories").as[List[String]]
      } yield Transaction(
        id,
        amount,
        merchant,
        LocalDateTime.parse(timestamp, DateTimeFormatter.ISO_LOCAL_DATE_TIME),
        description,
        categories
      )
    }.left.map(_.getMessage)
  }

  def close(): Unit = {
    s3Client.close()
  }
}

object S3VectorSearch {
  def apply(region: Region, bucketName: String, knowledgeBaseId: String): S3VectorSearch = {
    new S3VectorSearch(region, bucketName, knowledgeBaseId)
  }
}
