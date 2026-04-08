package txs

import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.{ReceiveMessageRequest, DeleteMessageRequest}
import software.amazon.awssdk.auth.credentials.{AwsBasicCredentials, StaticCredentialsProvider}
import software.amazon.awssdk.regions.Region
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.opensearch.core.IndexRequest
import org.opensearch.client.json.jackson.JacksonJsonpMapper
import org.opensearch.client.transport.rest_client.RestClientTransport
import org.apache.http.HttpHost
import org.apache.http.auth.{AuthScope, UsernamePasswordCredentials}
import org.apache.http.impl.client.BasicCredentialsProvider
import org.opensearch.client.RestClient
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import java.net.URI
import scala.jdk.CollectionConverters._

class TransactionConsumer(
  sqsClient: SqsClient,
  queueUrl: String,
  osClient: OpenSearchClient,
  indexName: String
):

  def consumeAndIndex(): Int =
    val request = ReceiveMessageRequest.builder()
      .queueUrl(queueUrl)
      .maxNumberOfMessages(10)
      .waitTimeSeconds(1)
      .build()

    val response = sqsClient.receiveMessage(request)
    val messages = response.messages().asScala.toList

    messages.foreach { message =>
      val tx = Transaction.fromJson(message.body())

      val indexRequest = IndexRequest.of[Transaction](i => i
        .index(indexName)
        .id(tx.id)
        .document(tx)
      )

      osClient.index(indexRequest)

      val deleteRequest = DeleteMessageRequest.builder()
        .queueUrl(queueUrl)
        .receiptHandle(message.receiptHandle())
        .build()

      sqsClient.deleteMessage(deleteRequest)
    }

    messages.size

object TransactionConsumer:
  def apply(
    sqsEndpoint: String,
    queueUrl: String,
    osEndpoint: String,
    indexName: String
  ): TransactionConsumer =
    val credentials = AwsBasicCredentials.create("test", "test")
    val sqsClient = SqsClient.builder()
      .endpointOverride(URI.create(sqsEndpoint))
      .region(Region.US_EAST_1)
      .credentialsProvider(StaticCredentialsProvider.create(credentials))
      .build()

    val credentialsProvider = new BasicCredentialsProvider()
    credentialsProvider.setCredentials(
      AuthScope.ANY,
      new UsernamePasswordCredentials("admin", "admin")
    )

    val restClient = RestClient.builder(HttpHost.create(osEndpoint))
      .setHttpClientConfigCallback(builder =>
        builder.setDefaultCredentialsProvider(credentialsProvider)
      )
      .build()

    val objectMapper = new ObjectMapper()
    objectMapper.registerModule(DefaultScalaModule)

    val transport = new RestClientTransport(restClient, new JacksonJsonpMapper(objectMapper))
    val osClient = new OpenSearchClient(transport)

    new TransactionConsumer(sqsClient, queueUrl, osClient, indexName)
