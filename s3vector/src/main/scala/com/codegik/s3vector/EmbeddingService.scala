package com.codegik.s3vector

import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest
import io.circe.parser.*
import io.circe.syntax.*
import io.circe.generic.auto.*
import scala.util.Try

class EmbeddingService(client: BedrockRuntimeClient) {

  def generateEmbedding(text: String): Either[String, Array[Float]] = {
    val requestBody = Map("inputText" -> text).asJson.noSpaces

    val request = InvokeModelRequest.builder()
      .modelId("amazon.titan-embed-text-v1")
      .body(SdkBytes.fromUtf8String(requestBody))
      .build()

    Try {
      val response = client.invokeModel(request)
      val responseBody = response.body().asUtf8String()

      parse(responseBody).flatMap { json =>
        json.hcursor.downField("embedding").as[List[Double]]
      }.map(_.map(_.toFloat).toArray)
        .left.map(_.getMessage)
    }.toEither
      .left.map(_.getMessage)
      .flatten
  }
}

object EmbeddingService {
  def apply(): EmbeddingService = {
    val client = BedrockRuntimeClient.builder().build()
    new EmbeddingService(client)
  }
}
