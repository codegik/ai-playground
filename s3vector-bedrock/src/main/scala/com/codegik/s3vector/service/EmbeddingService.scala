package com.codegik.s3vector.service

import com.codegik.s3vector.{EmbeddingService, TitanEmbedRequest}
import io.circe.generic.auto.*
import io.circe.parser.*
import io.circe.syntax.*
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest

import scala.util.{Try, Using}

case class TitanEmbedRequest(inputText: String)
case class TitanEmbedResponse(embedding: List[Double], inputTextTokenCount: Int)

class EmbeddingService(region: Region) {
  private val bedrockClient: BedrockRuntimeClient = BedrockRuntimeClient.builder()
    .region(region)
    .credentialsProvider(DefaultCredentialsProvider.create())
    .build()
  
  // Using Amazon Titan Embeddings V2 model (1024 dimensions)
  private val modelId = "amazon.titan-embed-text-v2:0"

  def generateEmbedding(text: String): Either[String, Array[Float]] = {
    Try {
      val requestBody = TitanEmbedRequest(inputText = text).asJson.noSpaces
      
      val request = InvokeModelRequest.builder()
        .modelId(modelId)
        .body(SdkBytes.fromUtf8String(requestBody))
        .build()

      val response = bedrockClient.invokeModel(request)
      val responseBody = response.body().asUtf8String()
      
      parse(responseBody).flatMap { json =>
        json.hcursor.downField("embedding").as[List[Double]]
      }.map(_.map(_.toFloat).toArray)
        .left.map(err => s"Failed to parse Bedrock response: ${err.getMessage}")
    }.toEither
      .left.map(err => s"Bedrock API call failed: ${err.getMessage}")
      .flatten
  }

  def close(): Unit = {
    bedrockClient.close()
  }
}

object EmbeddingService {
  def apply(region: Region = Region.US_EAST_1): EmbeddingService = {
    new EmbeddingService(region)
  }
}
