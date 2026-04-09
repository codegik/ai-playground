package txs

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.testcontainers.utility.DockerImageName
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest
import software.amazon.awssdk.auth.credentials.{AwsBasicCredentials, StaticCredentialsProvider}
import software.amazon.awssdk.regions.Region
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.opensearch.core.GetRequest
import org.opensearch.client.opensearch.indices.CreateIndexRequest
import org.opensearch.client.json.jackson.JacksonJsonpMapper
import org.opensearch.client.transport.rest_client.RestClientTransport
import org.apache.http.HttpHost
import org.apache.http.auth.{AuthScope, UsernamePasswordCredentials}
import org.apache.http.impl.client.BasicCredentialsProvider
import org.opensearch.client.RestClient
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import java.net.URI

class TransactionFlowIntegrationTest extends AnyFlatSpec with Matchers:

  "TransactionService and TransactionConsumer" should "post to SQS and index to OpenSearch" in {
    val localstack = new GenericContainer(DockerImageName.parse("localstack/localstack:3.0"))
    localstack.withEnv("SERVICES", "sqs")
    localstack.addExposedPort(4566)
    localstack.waitingFor(Wait.forLogMessage(".*Ready.*", 1))

    val opensearch = new GenericContainer(DockerImageName.parse("opensearchproject/opensearch:2.11.0"))
    opensearch.withEnv("discovery.type", "single-node")
    opensearch.withEnv("OPENSEARCH_INITIAL_ADMIN_PASSWORD", "Admin@123")
    opensearch.withEnv("DISABLE_SECURITY_PLUGIN", "true")
    opensearch.addExposedPort(9200)
    opensearch.waitingFor(Wait.forHttp("/_cluster/health").forPort(9200))

    localstack.start()
    opensearch.start()

    try {
      val sqsEndpoint = s"http://${localstack.getHost}:${localstack.getMappedPort(4566)}"
      val osEndpoint = s"http://${opensearch.getHost}:${opensearch.getMappedPort(9200)}"

      val credentials = AwsBasicCredentials.create("test", "test")
      val sqsClient = SqsClient.builder()
        .endpointOverride(URI.create(sqsEndpoint))
        .region(Region.US_EAST_1)
        .credentialsProvider(StaticCredentialsProvider.create(credentials))
        .build()

      val createQueueRequest = CreateQueueRequest.builder()
        .queueName("transactions")
        .build()
      val queueUrl = sqsClient.createQueue(createQueueRequest).queueUrl()

      val objectMapper = new ObjectMapper()
      objectMapper.registerModule(DefaultScalaModule)

      val restClient = RestClient.builder(HttpHost.create(osEndpoint)).build()
      val transport = new RestClientTransport(restClient, new JacksonJsonpMapper(objectMapper))
      val osClient = new OpenSearchClient(transport)

      val createIndexRequest = CreateIndexRequest.of(i => i.index("transactions"))
      osClient.indices().create(createIndexRequest)

      Thread.sleep(1000)

      val tx = Transaction("tx-001", 100.50, "USD", System.currentTimeMillis())
      val service = new TransactionService(sqsClient, queueUrl)
      val messageId = service.postTransaction(tx)

      messageId should not be empty

      val consumer = new TransactionConsumer(sqsClient, queueUrl, osClient, "transactions")

      var consumed = 0
      var attempts = 0
      while (consumed == 0 && attempts < 10) {
        consumed = consumer.consumeAndIndex()
        if (consumed == 0) {
          Thread.sleep(1000)
        }
        attempts += 1
      }

      consumed shouldBe 1

      Thread.sleep(1000)

      val getRequest = GetRequest.of(g => g.index("transactions").id("tx-001"))
      val getResponse = osClient.get(getRequest, classOf[Transaction])

      getResponse.found() shouldBe true
      val retrievedTx = getResponse.source()
      retrievedTx.id shouldBe "tx-001"
      retrievedTx.amount shouldBe 100.50
      retrievedTx.currency shouldBe "USD"

      restClient.close()
    } finally {
      localstack.stop()
      opensearch.stop()
    }
  }

  "TransactionConsumer" should "consume and index multiple transactions from SQS" in {
    val localstack = new GenericContainer(DockerImageName.parse("localstack/localstack:3.0"))
    localstack.withEnv("SERVICES", "sqs")
    localstack.addExposedPort(4566)
    localstack.waitingFor(Wait.forLogMessage(".*Ready.*", 1))

    val opensearch = new GenericContainer(DockerImageName.parse("opensearchproject/opensearch:2.11.0"))
    opensearch.withEnv("discovery.type", "single-node")
    opensearch.withEnv("OPENSEARCH_INITIAL_ADMIN_PASSWORD", "Admin@123")
    opensearch.withEnv("DISABLE_SECURITY_PLUGIN", "true")
    opensearch.addExposedPort(9200)
    opensearch.waitingFor(Wait.forHttp("/_cluster/health").forPort(9200))

    localstack.start()
    opensearch.start()

    try {
      val sqsEndpoint = s"http://${localstack.getHost}:${localstack.getMappedPort(4566)}"
      val osEndpoint  = s"http://${opensearch.getHost}:${opensearch.getMappedPort(9200)}"

      val credentials = AwsBasicCredentials.create("test", "test")
      val sqsClient = SqsClient.builder()
        .endpointOverride(URI.create(sqsEndpoint))
        .region(Region.US_EAST_1)
        .credentialsProvider(StaticCredentialsProvider.create(credentials))
        .build()

      val queueUrl = sqsClient.createQueue(
        CreateQueueRequest.builder().queueName("transactions-batch").build()
      ).queueUrl()

      val objectMapper = new ObjectMapper()
      objectMapper.registerModule(DefaultScalaModule)

      val restClient = RestClient.builder(HttpHost.create(osEndpoint)).build()
      val transport  = new RestClientTransport(restClient, new JacksonJsonpMapper(objectMapper))
      val osClient   = new OpenSearchClient(transport)

      osClient.indices().create(CreateIndexRequest.of(i => i.index("transactions-batch")))

      Thread.sleep(1000)

      val transactions = List(
        Transaction("tx-batch-001", 50.00,  "USD", System.currentTimeMillis()),
        Transaction("tx-batch-002", 75.25,  "EUR", System.currentTimeMillis()),
        Transaction("tx-batch-003", 200.00, "GBP", System.currentTimeMillis())
      )

      val service = new TransactionService(sqsClient, queueUrl)
      transactions.foreach(tx => service.postTransaction(tx) should not be empty)

      val consumer = new TransactionConsumer(sqsClient, queueUrl, osClient, "transactions-batch")

      var totalConsumed = 0
      var attempts = 0
      while (totalConsumed < transactions.size && attempts < 15) {
        totalConsumed += consumer.consumeAndIndex()
        if (totalConsumed < transactions.size) Thread.sleep(1000)
        attempts += 1
      }

      totalConsumed shouldBe transactions.size

      Thread.sleep(1000)

      transactions.foreach { tx =>
        val getResponse = osClient.get(
          GetRequest.of(g => g.index("transactions-batch").id(tx.id)),
          classOf[Transaction]
        )
        getResponse.found() shouldBe true
        val retrieved = getResponse.source()
        retrieved.id       shouldBe tx.id
        retrieved.amount   shouldBe tx.amount
        retrieved.currency shouldBe tx.currency
      }

      restClient.close()
    } finally {
      localstack.stop()
      opensearch.stop()
    }
  }
