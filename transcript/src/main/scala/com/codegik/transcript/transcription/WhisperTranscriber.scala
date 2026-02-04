package com.codegik.transcript.transcription

import scala.util.Try
import java.nio.file.{Files, Paths}
import java.io.{File, BufferedReader, InputStreamReader, PrintWriter}
import javax.sound.sampled.{AudioFileFormat, AudioFormat, AudioInputStream, AudioSystem}
import java.io.ByteArrayInputStream

/**
 * Real-time audio transcription
 * Supports both Vosk (fast) and faster-whisper (accurate)
 */
class WhisperTranscriber(engineType: String, modelParam: String):

  private val tempDir = Files.createTempDirectory("whisper-transcript").toFile
  tempDir.deleteOnExit()

  // Vosk components
  private var voskModel: Option[org.vosk.Model] = None
  private var voskRecognizer: Option[org.vosk.Recognizer] = None

  // Faster-whisper components
  private var whisperProcess: Option[Process] = None
  private var processInput: Option[PrintWriter] = None
  private var processOutput: Option[BufferedReader] = None

  private var usingVosk = false

  /**
   * Initialize based on engineType parameter
   */
  def initialize(): Try[Unit] = Try {
    engineType.toLowerCase match
      case "vosk" =>
        println(s"Initializing Vosk engine...")
        initializeVosk(modelParam)
      case "whisper" =>
        println(s"Initializing faster-whisper engine...")
        initializeFasterWhisper(modelParam)
      case _ =>
        throw RuntimeException(s"Unknown engine type: $engineType. Use 'vosk' or 'whisper'")
  }

  /**
   * Initialize Vosk
   */
  private def initializeVosk(modelPath: String): Unit =
    println(s"Loading Vosk model from: $modelPath")

    if !Files.exists(Paths.get(modelPath)) then
      throw RuntimeException(
        s"Vosk model not found at: $modelPath\n" +
        "Download from: https://alphacephei.com/vosk/models\n" +
        "Example: wget https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip"
      )

    val model = new org.vosk.Model(modelPath)
    val recognizer = new org.vosk.Recognizer(model, 16000)
    voskModel = Some(model)
    voskRecognizer = Some(recognizer)
    usingVosk = true

    println(s"✓ Vosk model loaded successfully")
    println("✓ Engine: Vosk (FAST - lower latency)")

  /**
   * Initialize faster-whisper server
   */
  private def initializeFasterWhisper(modelName: String): Unit =
    val scriptPath = "faster-whisper-server.py"

    println(s"Starting faster-whisper server (model: $modelName)...")
    println("First run may download the model (~75-500 MB depending on model)...")

    val processBuilder = new ProcessBuilder("python3", scriptPath, modelName)
    processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT)

    val process = processBuilder.start()
    whisperProcess = Some(process)

    processInput = Some(new PrintWriter(process.getOutputStream, true))
    processOutput = Some(new BufferedReader(new InputStreamReader(process.getInputStream)))

    // Wait for READY signal
    val ready = processOutput.get.readLine()
    if ready != "READY" then
      throw RuntimeException(s"faster-whisper server failed to start. Got: $ready")

    usingVosk = false
    println(s"✓ faster-whisper ready!")
    println(s"✓ Engine: faster-whisper (ACCURATE - Google-like quality)")

  /**
   * Transcribe audio data
   */
  def transcribe(
    audioData: Array[Byte],
    detectLanguage: Boolean = true,
    language: Option[String] = None
  ): Try[TranscriptionResult] = Try {
    if audioData.isEmpty then
      TranscriptionResult("", Some("en"), 0.0f)
    else if usingVosk then
      transcribeWithVosk(audioData)
    else
      transcribeWithFasterWhisper(audioData)
  }

  /**
   * Transcribe using Vosk
   */
  private def transcribeWithVosk(audioData: Array[Byte]): TranscriptionResult =
    voskRecognizer match
      case Some(rec) =>
        val accepted = rec.acceptWaveForm(audioData, audioData.length)
        val resultJson = rec.getFinalResult()
        val result = parseVoskResult(resultJson)

        if result.text.nonEmpty then
          rec.reset()

        result
      case None =>
        TranscriptionResult("", Some("en"), 0.0f)

  /**
   * Transcribe using faster-whisper
   */
  private def transcribeWithFasterWhisper(audioData: Array[Byte]): TranscriptionResult =
    val tempFile = File.createTempFile("audio-", ".wav", tempDir)
    saveAsWav(audioData, tempFile)

    try {
      processInput.get.println(tempFile.getAbsolutePath)
      val response = processOutput.get.readLine()
      if response == null then
        throw RuntimeException("faster-whisper server stopped responding")

      parseWhisperOutput(response)
    } finally {
      tempFile.delete()
    }

  /**
   * Save audio bytes as WAV file
   */
  private def saveAsWav(audioData: Array[Byte], file: File): Unit =
    val audioFormat = AudioFormat(16000.0f, 16, 1, true, false)
    val audioInputStream = AudioInputStream(
      ByteArrayInputStream(audioData),
      audioFormat,
      audioData.length / audioFormat.getFrameSize
    )
    AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, file)

  /**
   * Parse Vosk JSON result
   */
  private def parseVoskResult(jsonResult: String): TranscriptionResult =
    try {
      val parser = com.google.gson.JsonParser.parseString(jsonResult)
      val jsonObject = parser.getAsJsonObject
      val text = if jsonObject.has("text") then jsonObject.get("text").getAsString else ""
      TranscriptionResult(text.trim, Some("en"), 1.0f)
    } catch {
      case e: Exception => TranscriptionResult("", Some("en"), 0.0f)
    }

  /**
   * Parse faster-whisper JSON output
   */
  private def parseWhisperOutput(jsonOutput: String): TranscriptionResult =
    val textPattern = """"text":\s*"([^"]*)"""".r
    val langPattern = """"language":\s*"([^"]*)"""".r

    val text = textPattern.findFirstMatchIn(jsonOutput).map(_.group(1).trim).getOrElse("")
    val lang = langPattern.findFirstMatchIn(jsonOutput).map(_.group(1))

    TranscriptionResult(text, lang, 1.0f)

  /**
   * Release resources
   */
  def close(): Unit =
    if usingVosk then
      voskRecognizer.foreach(_.close())
      voskModel.foreach(_.close())
    else
      try {
        processInput.foreach { writer =>
          writer.println("QUIT")
          writer.close()
        }
        processOutput.foreach(_.close())
        whisperProcess.foreach { p =>
          p.waitFor()
          p.destroy()
        }
      } catch {
        case _: Exception => // Ignore cleanup errors
      }

    tempDir.listFiles().foreach(_.delete())
    tempDir.delete()
    println("✓ Transcription resources released")

/**
 * Result of transcription
 */
case class TranscriptionResult(
  text: String,
  language: Option[String],
  confidence: Float
)
