package txs

import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import java.net.URI

class TransactionService(sqsClient: SqsClient, queueUrl: String):

  def postTransaction(tx: Transaction): String =
    val json = Transaction.toJson(tx)
    val request = SendMessageRequest.builder()
      .queueUrl(queueUrl)
      .messageBody(json)
      .build()

    val response = sqsClient.sendMessage(request)
    response.messageId()

object TransactionService:
  def apply(endpoint: String, queueUrl: String): TransactionService =
    val credentials = AwsBasicCredentials.create("test", "test")
    val sqsClient = SqsClient.builder()
      .endpointOverride(URI.create(endpoint))
      .region(Region.US_EAST_1)
      .credentialsProvider(StaticCredentialsProvider.create(credentials))
      .build()

    new TransactionService(sqsClient, queueUrl)
