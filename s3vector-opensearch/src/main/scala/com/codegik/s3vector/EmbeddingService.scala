package com.codegik.s3vector

import io.circe.parser.*
import io.circe.syntax.*
import io.circe.generic.auto.*
import org.apache.hc.client5.http.classic.methods.HttpPost
import org.apache.hc.client5.http.impl.classic.{CloseableHttpClient, HttpClients}
import org.apache.hc.core5.http.ContentType
import org.apache.hc.core5.http.io.entity.StringEntity
import scala.io.Source
import scala.util.{Try, Using}

case class OllamaEmbedRequest(model: String, prompt: String)
case class OllamaEmbedResponse(embedding: List[Double])

class EmbeddingService(ollamaEndpoint: String) {
  private val httpClient: CloseableHttpClient = HttpClients.createDefault()

  def generateEmbedding(text: String): Either[String, Array[Float]] = {
    val requestBody = OllamaEmbedRequest(
      model = "nomic-embed-text",
      prompt = text
    ).asJson.noSpaces

    Try {
      val request = new HttpPost(s"$ollamaEndpoint/api/embeddings")
      request.setHeader("Content-Type", "application/json")
      request.setEntity(new StringEntity(requestBody, ContentType.APPLICATION_JSON))

      Using(httpClient.execute(request)) { response =>
        val statusCode = response.getCode
        val responseBody = Using(Source.fromInputStream(response.getEntity.getContent))(_.mkString).get

        if (statusCode >= 200 && statusCode < 300) {
          parse(responseBody).flatMap { json =>
            json.hcursor.downField("embedding").as[List[Double]]
          }.map(_.map(_.toFloat).toArray)
            .left.map(_.getMessage)
        } else {
          Left(s"Request failed with status $statusCode: $responseBody")
        }
      }.get
    }.toEither
      .left.map(_.getMessage)
      .flatten
  }

  def close(): Unit = {
    httpClient.close()
  }
}

object EmbeddingService {
  def apply(ollamaEndpoint: String = "http://localhost:11434"): EmbeddingService = {
    new EmbeddingService(ollamaEndpoint)
  }
}
