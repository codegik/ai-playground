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

    // Parse arguments: [engine] [model_path_or_name]
    // engine: "vosk" or "whisper" (default: auto-detect)
    // model: model path for vosk, or model name for whisper
    val (engineType, modelParam) = parseArguments(args)

    println(s"Engine: $engineType")
    println("=" * 60)

    // Create the transcription engine
    val engine = RealtimeTranscriptionEngine(
      engineType = engineType,
      modelParam = modelParam,
      chunkDurationMs = 1000 // Process audio in 1-second chunks for real-time
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
   * Auto-detect Vosk model in models directory
   */
  private def findVoskModel(): Option[String] =
    val modelsDir = new java.io.File("models")

    if !modelsDir.exists() || !modelsDir.isDirectory then
      return None

    // Look for any directory in models/ that looks like a Vosk model
    val modelDirs = modelsDir.listFiles()
      .filter(_.isDirectory)
      .filter { dir =>
        // Check if it has typical Vosk model structure (am, conf, graph folders)
        val hasAm = new java.io.File(dir, "am").exists()
        val hasConf = new java.io.File(dir, "conf").exists()
        val hasGraph = new java.io.File(dir, "graph").exists()
        hasAm || hasConf || hasGraph
      }

    if modelDirs.nonEmpty then
      val selectedModel = s"models/${modelDirs.head.getName}"
      println(s"Auto-detected model: $selectedModel")
      Some(selectedModel)
    else
      None

  /**
   * Print instructions for downloading Vosk models
   */
  private def printModelInstructions(): Unit =
    println()
    println("ERROR: Vosk model not found!")
    println()
    println("This system uses Vosk for REAL-TIME transcription (much faster than Whisper).")
    println()
    println("Installation instructions:")
    println("  1. Create models directory:")
    println("     mkdir -p models")
    println("     cd models")
    println()
    println("  2. Download a Vosk model:")
    println()
    println("  English (US) - Small (~40 MB) - RECOMMENDED FOR REAL-TIME:")
    println("     wget https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip")
    println("     unzip vosk-model-small-en-us-0.15.zip")
    println()
    println("  English (US) - Large (~1.8 GB) - Better accuracy:")
    println("     wget https://alphacephei.com/vosk/models/vosk-model-en-us-0.22.zip")
    println("     unzip vosk-model-en-us-0.22.zip")
    println()
    println("  Other languages available at:")
    println("     https://alphacephei.com/vosk/models")
    println()
    println("  3. Run the application:")
    println("     sbt run")
    println()
    println("  Or specify custom model:")
    println("     sbt \"run models/vosk-model-en-us-0.22\"")
    println()
