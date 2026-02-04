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

    // Model name (tiny, base, small, medium, large)
    val modelName = args.headOption.getOrElse("base")

    // Create the transcription engine
    val engine = RealtimeTranscriptionEngine(
      modelName = modelName,
      chunkDurationMs = 300 // Process audio in 3-second chunks
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
    println("ERROR: Whisper not installed!")
    println()
    println("This system uses OpenAI's Whisper via Python.")
    println()
    println("Installation instructions:")
    println("  1. Install Python 3 (if not already installed)")
    println()
    println("  2. Install Whisper:")
    println("     pip install openai-whisper")
    println()
    println("  Or with conda:")
    println("     conda install -c conda-forge openai-whisper")
    println()
    println("Available models (downloaded automatically on first use):")
    println("  - tiny   (~75 MB)  - Fastest, less accurate")
    println("  - base   (~142 MB) - Good balance (RECOMMENDED)")
    println("  - small  (~466 MB) - Better accuracy")
    println("  - medium (~1.5 GB) - High accuracy")
    println("  - large  (~2.9 GB) - Best accuracy, slowest")
    println()
    println("After installation, run:")
    println("  sbt run                  # Uses 'base' model")
    println("  sbt \"run tiny\"         # Uses 'tiny' model for faster processing")
    println("  sbt \"run small\"        # Uses 'small' model for better accuracy")
    println()
