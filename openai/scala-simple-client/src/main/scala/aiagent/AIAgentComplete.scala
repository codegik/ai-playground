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

// Circe configuration for snake_case to camelCase mapping
given Configuration = Configuration.default.withSnakeCaseMemberNames

// Constants
object Constants:
  val DefaultModel = "o4-mini"

// OpenAI API Models
case class ChatMessage(
  role: String,
  content: String,
  refusal: Option[String] = None,
  annotations: Option[List[String]] = None
) derives Encoder.AsObject, Decoder

case class ChatCompletionRequest(
  model: String,
  messages: List[ChatMessage],
) derives Encoder.AsObject, Decoder

case class Choice(
  index: Int,
  message: ChatMessage,
  finish_reason: Option[String]
) derives Encoder.AsObject, Decoder

case class Usage(
  prompt_tokens: Int,
  completion_tokens: Int,
  total_tokens: Int
) derives Encoder.AsObject, Decoder

case class ChatCompletionResponse(
  id: String,
  `object`: String,
  created: Long,
  model: String,
  choices: List[Choice],
  usage: Usage
) derives Encoder.AsObject, Decoder

case class ResponsesRequest(
  model: String,
  input: String,
) derives Encoder.AsObject, Decoder

// Responses API Models
case class ContentAnnotation(
  // Add fields as needed based on actual annotation structure
) derives Encoder.AsObject, Decoder

case class OutputContent(
  `type`: Option[String],
  text: Option[String],
  annotations: List[ContentAnnotation]
) derives Encoder.AsObject, Decoder

case class ResponseMessage(
  `type`: Option[String],
  id: Option[String],
  status: Option[String],
  role: Option[String],
  content: Option[List[OutputContent]]
) derives Encoder.AsObject, Decoder

case class InputTokensDetails(
  cached_tokens: Int
) derives Encoder.AsObject, Decoder

case class OutputTokensDetails(
  reasoning_tokens: Int
) derives Encoder.AsObject, Decoder

case class ResponseUsage(
  input_tokens: Int,
  input_tokens_details: InputTokensDetails,
  output_tokens: Int,
  output_tokens_details: OutputTokensDetails,
  total_tokens: Int
) derives Encoder.AsObject, Decoder

case class ResponseReasoning(
  effort: Option[String],
  summary: Option[String]
) derives Encoder.AsObject, Decoder

case class TextFormat(
  `type`: String
) derives Encoder.AsObject, Decoder

case class ResponseText(
  format: TextFormat,
  verbosity: String  // Add the missing verbosity field from JSON
) derives Encoder.AsObject, Decoder

case class ResponsesResponse(
  id: String,
  `object`: String,
  created_at: Long,
  status: String,
  background: Boolean,
  error: Option[String],
  incompleteDetails: Option[String],
  instructions: Option[String],
  maxOutputTokens: Option[Int],
  maxToolCalls: Option[Int],
  model: String,
  output: List[ResponseMessage],
  parallel_tool_calls: Boolean,
  previousResponseId: Option[String],
  promptCacheKey: Option[String],
  reasoning: ResponseReasoning,
  safetyIdentifier: Option[String],
  service_tier: String,
  store: Boolean,
  temperature: Double,
  text: ResponseText,
  tool_choice: String,
  tools: List[String],
  top_logprobs: Int,
  top_p: Double,
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

    client.expect[ChatCompletionResponse](req)

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

    client.expect[ResponsesResponse](req).map { response =>
      response.output
        .find(_.content.isDefined)  // Find the output that has content
        .flatMap(_.content)
        .flatMap(_.headOption)
        .flatMap(_.text)
        .getOrElse("No content")
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
    )

    openAIClient.chatCompletion(request).map(_.choices.headOption.map(_.message.content).getOrElse("No response"))

object AIAgent:
  def create[F[_]: Async](client: Client[F], config: AIAgentConfig): AIAgent[F] =
    val openAIClient = OpenAIClient(client, config.openAIApiKey)
    new AIAgent[F](openAIClient, config)
