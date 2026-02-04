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

    // Model path (user should download the model first)
    val modelPath = args.headOption.getOrElse("models/ggml-base.bin")

    if !Files.exists(Paths.get(modelPath)) then
      printModelInstructions()
      System.exit(1)

    // Create the transcription engine
    val engine = RealtimeTranscriptionEngine(
      modelPath = modelPath,
      chunkDurationMs = 3000 // Process audio in 3-second chunks
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
   * Print instructions for downloading Whisper models
   */
  private def printModelInstructions(): Unit =
    println()
    println("ERROR: Model file not found!")
    println()
    println("You need to download a Whisper model first.")
    println()
    println("Available models (from fastest/smallest to slowest/largest):")
    println("  - ggml-tiny.bin     (~75 MB)  - Fastest, less accurate")
    println("  - ggml-base.bin     (~142 MB) - Good balance (RECOMMENDED)")
    println("  - ggml-small.bin    (~466 MB) - Better accuracy")
    println("  - ggml-medium.bin   (~1.5 GB) - High accuracy")
    println("  - ggml-large.bin    (~2.9 GB) - Best accuracy, slowest")
    println()
    println("Download instructions:")
    println("  1. Create a 'models' directory in the project root:")
    println("     mkdir -p models")
    println()
    println("  2. Download a model (example for base model):")
    println("     wget https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base.bin -O models/ggml-base.bin")
    println()
    println("  Or download from: https://huggingface.co/ggerganov/whisper.cpp/tree/main")
    println()
    println("  3. Run the application with the model path:")
    println("     sbt \"run models/ggml-base.bin\"")
    println()
