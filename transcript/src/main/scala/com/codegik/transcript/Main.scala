package com.codegik.transcript

import com.codegik.transcript.transcription.TranscriptionResult
import scala.util.{Success, Failure}
import scala.concurrent.ExecutionContext
import java.nio.file.{Paths, Files}

/**
 * Main application for real-time audio transcription
 */
object Main:

  given ExecutionContext = ExecutionContext.global

  def main(args: Array[String]): Unit =
    println("=" * 60)
    println("Real-Time Audio Transcription System")
    println("=" * 60)

    // Model name - use tiny for speed (base for better accuracy)
    val modelName = args.headOption.getOrElse("tiny")

    // Create the transcription engine
    val engine = RealtimeTranscriptionEngine(
      modelPath = modelName,
      chunkDurationMs = 2000 // Process audio in 2-second chunks
    )

    // Initialize the engine
    engine.initialize() match
      case Success(_) =>
        println("✓ Engine initialized successfully")

        // Add shutdown hook for graceful cleanup
        sys.addShutdownHook {
          println("\nShutting down...")
          engine.close()
        }

        // Start real-time transcription
        engine.start { result =>
          displayTranscription(result)
        } match
          case Success(_) =>
            // Keep the application running
            println("\nPress Ctrl+C to stop transcription")

            // Wait indefinitely
            while true do
              Thread.sleep(1000)

          case Failure(exception) =>
            System.err.println(s"Failed to start transcription: ${exception.getMessage}")
            exception.printStackTrace()
            engine.close()
            System.exit(1)

      case Failure(exception) =>
        System.err.println(s"Failed to initialize: ${exception.getMessage}")
        exception.printStackTrace()
        System.exit(1)

  /**
   * Display transcription result with formatting
   */
  private def displayTranscription(result: TranscriptionResult): Unit =
    val langInfo = result.language.map(lang => s"[$lang]").getOrElse("[unknown]")
    val timestamp = java.time.LocalTime.now().toString.take(8)

    println(f"$timestamp $langInfo%10s | ${result.text}")

  /**
   * Print instructions for installing Whisper
   */
  private def printModelInstructions(): Unit =
    println()
    println("ERROR: Whisper not installed or failed to start!")
    println()
    println("This system uses Whisper with a PERSISTENT process for speed.")
    println("The model is loaded ONCE and kept in memory (10-20x faster!).")
    println()
    println("Installation:")
    println("  pip3 install openai-whisper")
    println()
    println("Available models:")
    println("  - tiny   (~75 MB)  - FASTEST (recommended for real-time)")
    println("  - base   (~142 MB) - Good balance")
    println("  - small  (~466 MB) - Better accuracy")
    println()
    println("Run with:")
    println("  sbt run              # Uses tiny model (fastest)")
    println("  sbt \"run base\"     # Uses base model")
    println("  sbt \"run small\"    # Uses small model")
    println()
