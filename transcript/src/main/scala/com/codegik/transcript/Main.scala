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
   * Parse command-line arguments
   * Usage: sbt run [engine] [model]
   *   engine: vosk, whisper, or auto (default: auto)
   *   model: model path for vosk, model name for whisper (default: auto-detect)
   */
  private def parseArguments(args: Array[String]): (String, String) =
    args.toList match
      case Nil =>
        // No args - auto-detect
        findVoskModel() match
          case Some(modelPath) => ("vosk", modelPath)
          case None => ("whisper", "base")

      case engine :: model :: _ if engine == "vosk" || engine == "whisper" =>
        // Both engine and model specified (e.g., "whisper medium")
        (engine, model)

      case engine :: Nil if engine == "vosk" || engine == "whisper" =>
        // Engine specified, auto-detect model
        engine match
          case "vosk" =>
            findVoskModel() match
              case Some(modelPath) => ("vosk", modelPath)
              case None =>
                printVoskModelInstructions()
                System.exit(1)
                ("vosk", "")
          case "whisper" => ("whisper", "base")
          case _ => ("whisper", "base")


      case other =>
        // First arg is probably a model path/name - auto-detect engine
        val param = other.head
        if Files.exists(Paths.get(param)) then
          ("vosk", param)
        else
          ("whisper", param)

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
      Some(selectedModel)
    else
      None

  /**
   * Print instructions for downloading Vosk models
   */
  private def printVoskModelInstructions(): Unit =
    println()
    println("ERROR: Vosk model not found!")
    println()
    println("Download instructions:")
    println("  mkdir -p models && cd models")
    println("  wget https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip")
    println("  unzip vosk-model-small-en-us-0.15.zip")
    println("  cd ..")
    println()

  /**
   * Print usage instructions
   */
  private def printModelInstructions(): Unit =
    println()
    println("Real-Time Audio Transcription System")
    println("=" * 60)
    println()
    println("USAGE:")
    println("  sbt run                          # Auto-detect engine")
    println("  sbt \"run vosk\"                  # Use Vosk (fast)")
    println("  sbt \"run whisper\"               # Use faster-whisper (accurate)")
    println("  sbt \"run vosk <model_path>\"     # Use specific Vosk model")
    println("  sbt \"run whisper <model_name>\"  # Use specific Whisper model")
    println()
    println("=" * 60)
    println()
    println("VOSK SETUP (Fast, less accurate):")
    println("  1. Download model:")
    println("     mkdir -p models && cd models")
    println("     wget https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip")
    println("     unzip vosk-model-small-en-us-0.15.zip")
    println("     cd ..")
    println("  2. Run:")
    println("     sbt \"run vosk\"")
    println()
    println("FASTER-WHISPER SETUP (Slower, Google-like accuracy):")
    println("  1. Install:")
    println("     pip3 install faster-whisper")
    println("  2. Run:")
    println("     sbt \"run whisper\"")
    println()
    println("Available Whisper models: tiny, base, small, medium, large")
    println("More Vosk models: https://alphacephei.com/vosk/models")
    println()


