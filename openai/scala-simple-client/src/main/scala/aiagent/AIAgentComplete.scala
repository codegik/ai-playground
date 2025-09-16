package aiagent

import cats.effect.*
import cats.syntax.all.*
import io.circe.*
import io.circe.derivation.*
import org.http4s.*
import org.http4s.circe.*
import org.http4s.client.*
import org.http4s.headers.*
import org.http4s.syntax.literals.*

// Constants
object Constants:
  val DefaultModel = "o4-mini"

// OpenAI API Models
case class ChatMessage(
  role: String,
  content: String
) derives Encoder.AsObject, Decoder

case class ChatCompletionRequest(
  model: String,
  messages: List[ChatMessage],
  maxTokens: Option[Int] = None,
  temperature: Option[Double] = None,
  stream: Boolean = false
) derives Encoder.AsObject, Decoder

case class ResponsesRequest(
  model: String,
  input: String,
) derives Encoder.AsObject, Decoder


case class Choice(
  index: Int,
  message: ChatMessage,
  finishReason: Option[String]
) derives Encoder.AsObject, Decoder

case class Usage(
  promptTokens: Int,
  completionTokens: Int,
  totalTokens: Int
) derives Encoder.AsObject, Decoder

case class ChatCompletionResponse(
  id: String,
  `object`: String,
  created: Long,
  model: String,
  choices: List[Choice],
  usage: Usage
) derives Encoder.AsObject, Decoder
// Responses API Models
case class ContentAnnotation(
  // Add fields as needed based on actual annotation structure
) derives Encoder.AsObject, Decoder

case class OutputContent(
  `type`: String,
  text: String,
  annotations: List[ContentAnnotation]
) derives Encoder.AsObject, Decoder

case class ResponseMessage(
  `type`: String,
  id: String,
  status: String,
  role: String,
  content: List[OutputContent]
) derives Encoder.AsObject, Decoder

case class InputTokensDetails(
  cachedTokens: Int
) derives Encoder.AsObject, Decoder

case class OutputTokensDetails(
  reasoningTokens: Int
) derives Encoder.AsObject, Decoder

case class ResponseUsage(
  inputTokens: Int,
  inputTokensDetails: InputTokensDetails,
  outputTokens: Int,
  outputTokensDetails: OutputTokensDetails,
  totalTokens: Int
) derives Encoder.AsObject, Decoder

case class ResponseReasoning(
  effort: Option[String],
  summary: Option[String]
) derives Encoder.AsObject, Decoder

case class TextFormat(
  `type`: String
) derives Encoder.AsObject, Decoder

case class ResponseText(
  format: TextFormat
) derives Encoder.AsObject, Decoder

case class ResponsesResponse(
  id: String,
  `object`: String,
  createdAt: Long,
  status: String,
  error: Option[String],
  incompleteDetails: Option[String],
  instructions: Option[String],
  maxOutputTokens: Option[Int],
  model: String,
  output: List[ResponseMessage],
  parallelToolCalls: Boolean,
  previousResponseId: Option[String],
  reasoning: ResponseReasoning,
  store: Boolean,
  temperature: Double,
  text: ResponseText,
  toolChoice: String,
  tools: List[String],
  topP: Double,
  truncation: String,
  usage: ResponseUsage,
  user: Option[String],
  metadata: Map[String, String]
) derives Encoder.AsObject, Decoder

// OpenAI Client
class OpenAIClient[F[_]: Async](client: Client[F], apiKey: String):
  private val baseUrl = uri"https://api.openai.com/v1"

  given EntityEncoder[F, ChatCompletionRequest] = jsonEncoderOf[F, ChatCompletionRequest]
  given EntityDecoder[F, ChatCompletionResponse] = jsonOf[F, ChatCompletionResponse]
  given EntityEncoder[F, ResponsesRequest] = jsonEncoderOf[F, ResponsesRequest]
  given EntityDecoder[F, ResponsesResponse] = jsonOf[F, ResponsesResponse]

  def chatCompletion(request: ChatCompletionRequest): F[ChatCompletionResponse] =
    val req = Request[F](
      method = Method.POST,
      uri = baseUrl / "chat" / "completions",
      headers = Headers(
        Authorization(Credentials.Token(AuthScheme.Bearer, apiKey)),
        `Content-Type`(MediaType.application.json)
      )
    ).withEntity(request)

    client.expect[ChatCompletionResponse](req).flatTap { response =>
      Async[F].delay(println(s"Full OpenAI API Response: $response"))
    }

  def sendMessage(message: String, model: String = Constants.DefaultModel): F[String] =
    val request = ResponsesRequest(model = model, input = message)
    val req = Request[F](
      method = Method.POST,
      uri = baseUrl / "responses",
      headers = Headers(
        Authorization(Credentials.Token(AuthScheme.Bearer, apiKey)),
        `Content-Type`(MediaType.application.json)
      )
    ).withEntity(request)

    client.expect[ResponsesResponse](req).flatTap { response =>
      Async[F].delay(println(s"Full OpenAI API Response: $response"))
    }.map { response =>
      // Extract text content from the response structure
      response.output.headOption
        .flatMap(_.content.headOption)
        .map(_.text)
        .getOrElse("No response")
    }

object OpenAIClient:
  def apply[F[_]: Async](client: Client[F], apiKey: String): OpenAIClient[F] =
    new OpenAIClient[F](client, apiKey)

// AI Agent Configuration and Implementation
case class AIAgentConfig(
  openAIApiKey: String,
  defaultModel: String = Constants.DefaultModel,
  maxTokens: Int = 1000,
  temperature: Double = 0.7
)

class AIAgent[F[_]: Async](
  openAIClient: OpenAIClient[F],
  config: AIAgentConfig
):

  def ask(question: String): F[String] =
    openAIClient.sendMessage(question, config.defaultModel)

  def chat(messages: List[ChatMessage]): F[String] =
    val request = ChatCompletionRequest(
      model = config.defaultModel,
      messages = messages,
      maxTokens = Some(config.maxTokens),
      temperature = Some(config.temperature)
    )

    openAIClient.chatCompletion(request).map(_.choices.headOption.map(_.message.content).getOrElse("No response"))

object AIAgent:
  def create[F[_]: Async](client: Client[F], config: AIAgentConfig): AIAgent[F] =
    val openAIClient = OpenAIClient(client, config.openAIApiKey)
    new AIAgent[F](openAIClient, config)
