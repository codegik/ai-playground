package aiagent

import cats.effect.*
import org.http4s.ember.client.EmberClientBuilder

import scala.util.Properties

object AIAgentApp extends IOApp.Simple:
  
  def run: IO[Unit] =
    EmberClientBuilder.default[IO].build.use { client =>
      for {
        apiKey <- IO.fromOption(Properties.envOrNone("OPENAI_API_KEY"))(
          new RuntimeException("OPENAI_API_KEY environment variable not set")
        )
        config = AIAgentConfig(openAIApiKey = apiKey)
        agent = AIAgent.create(client, config)
        
        _ <- IO.println("AI Agent initialized. Starting conversation...")
        _ <- IO.println("=" * 50)
        
        // Example: Simple question
        _ <- IO.println("Asking: What is Scala?")
        response1 <- agent.ask("What is Scala programming language in one sentence?")
        _ <- IO.println(s"Agent: $response1")
        _ <- IO.print("\n")
        
        // Example: Multi-turn conversation
        _ <- IO.println("Starting a conversation about functional programming...")
        messages = List(
          ChatMessage("system", "You are a helpful programming assistant."),
          ChatMessage("user", "Explain the benefits of functional programming"),
          ChatMessage("assistant", "Functional programming offers benefits like immutability, referential transparency, and easier reasoning about code."),
          ChatMessage("user", "How does Cats Effect help with functional programming in Scala?")
        )
        response2 <- agent.chat(messages)
        _ <- IO.println(s"Agent: $response2")
        
      } yield ()
    }.handleErrorWith { error =>
      IO.println(s"Error: ${error.getMessage}")
    }
