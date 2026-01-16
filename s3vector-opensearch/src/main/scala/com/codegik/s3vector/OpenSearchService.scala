package com.codegik.s3vector

import cats.implicits.*
import io.circe.*
import io.circe.parser.*
import org.apache.hc.client5.http.classic.methods.{HttpGet, HttpPost, HttpPut}
import org.apache.hc.client5.http.impl.classic.{CloseableHttpClient, HttpClients}
import org.apache.hc.core5.http.ContentType
import org.apache.hc.core5.http.io.entity.StringEntity

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import scala.io.Source
import scala.util.{Try, Using}

class OpenSearchService(endpoint: String) {
  private val httpClient: CloseableHttpClient = HttpClients.createDefault()
  private val indexName = "transactions"

  def createIndex(): Either[String, String] = {
    val indexConfig = Json.obj(
      "settings" -> Json.obj(
        "index.knn" -> Json.fromBoolean(true)
      ),
      "mappings" -> Json.obj(
        "properties" -> Json.obj(
          "transaction_id" -> Json.obj("type" -> Json.fromString("keyword")),
          "amount" -> Json.obj("type" -> Json.fromString("double")),
          "merchant_name" -> Json.obj("type" -> Json.fromString("text")),
          "timestamp" -> Json.obj("type" -> Json.fromString("date")),
          "description" -> Json.obj("type" -> Json.fromString("text")),
          "categories" -> Json.obj("type" -> Json.fromString("keyword")),
          "embedding" -> Json.obj(
            "type" -> Json.fromString("knn_vector"),
            "dimension" -> Json.fromInt(768)
          )
        )
      )
    )

    executeRequest(
      new HttpPut(s"$endpoint/$indexName"),
      Some(indexConfig.noSpaces)
    )
  }

  def indexTransaction(transaction: Transaction, embedding: Array[Float]): Either[String, String] = {
    val doc = Json.obj(
      "transaction_id" -> Json.fromString(transaction.id),
      "amount" -> Json.fromDoubleOrNull(transaction.amount),
      "merchant_name" -> Json.fromString(transaction.merchantName),
      "timestamp" -> Json.fromString(transaction.timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)),
      "description" -> Json.fromString(transaction.description),
      "categories" -> Json.fromValues(transaction.categories.map(Json.fromString)),
      "embedding" -> Json.fromValues(embedding.map(f => Json.fromFloatOrNull(f)))
    )

    executeRequest(
      new HttpPost(s"$endpoint/$indexName/_doc/${transaction.id}"),
      Some(doc.noSpaces)
    )
  }

  def search(queryEmbedding: Array[Float], k: Int = 5): Either[String, List[Transaction]] = {
    val searchQuery = Json.obj(
      "size" -> Json.fromInt(k),
      "query" -> Json.obj(
        "knn" -> Json.obj(
          "embedding" -> Json.obj(
            "vector" -> Json.fromValues(queryEmbedding.map(f => Json.fromFloatOrNull(f))),
            "k" -> Json.fromInt(k)
          )
        )
      )
    )

    executeRequest(
      new HttpPost(s"$endpoint/$indexName/_search"),
      Some(searchQuery.noSpaces)
    ).flatMap { response =>
      parseSearchResults(response)
    }
  }

  private def executeRequest(request: HttpPost | HttpPut | HttpGet, body: Option[String] = None): Either[String, String] = {
    Try {
      request.setHeader("Content-Type", "application/json")

      body.foreach { b =>
        request match {
          case post: HttpPost => post.setEntity(new StringEntity(b, ContentType.APPLICATION_JSON))
          case put: HttpPut => put.setEntity(new StringEntity(b, ContentType.APPLICATION_JSON))
          case _ => ()
        }
      }

      Using(httpClient.execute(request)) { response =>
        val statusCode = response.getCode
        val responseBody = Using(Source.fromInputStream(response.getEntity.getContent))(_.mkString).get

        if (statusCode >= 200 && statusCode < 300) {
          responseBody
        } else {
          throw new RuntimeException(s"Request failed with status $statusCode: $responseBody")
        }
      }.get
    }.toEither.left.map(_.getMessage)
  }

  private def parseSearchResults(response: String): Either[String, List[Transaction]] = {
    parse(response).flatMap { json =>
      json.hcursor
        .downField("hits")
        .downField("hits")
        .as[List[Json]]
        .flatMap { hits =>
          hits.traverse { hit =>
            val source = hit.hcursor.downField("_source")
            for {
              id <- source.downField("transaction_id").as[String]
              amount <- source.downField("amount").as[Double]
              merchant <- source.downField("merchant_name").as[String]
              timestamp <- source.downField("timestamp").as[String]
              description <- source.downField("description").as[String]
              categories <- source.downField("categories").as[List[String]]
            } yield Transaction(
              id,
              amount,
              merchant,
              LocalDateTime.parse(timestamp, DateTimeFormatter.ISO_LOCAL_DATE_TIME),
              description,
              categories
            )
          }
        }
    }.left.map(_.getMessage)
  }

  def close(): Unit = {
    httpClient.close()
  }
}

object OpenSearchService {
  def apply(endpoint: String): OpenSearchService = {
    new OpenSearchService(endpoint)
  }
}
