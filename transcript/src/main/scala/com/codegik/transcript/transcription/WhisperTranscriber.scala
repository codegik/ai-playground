package com.codegik.transcript.transcription

import scala.util.{Try, Success, Failure}
import scala.sys.process.*
import java.nio.file.{Path, Paths, Files}
import java.io.{File, FileOutputStream}
import javax.sound.sampled.{AudioFileFormat, AudioFormat, AudioInputStream, AudioSystem}
import java.io.ByteArrayInputStream

/**
 * Real-time audio transcription using Whisper via Python
 * Requires: pip install openai-whisper
 */
class WhisperTranscriber(modelName: String = "base"):

  private val tempDir = Files.createTempDirectory("whisper-transcript").toFile
  tempDir.deleteOnExit()
  
  /**
   * Initialize the Whisper model (check if Python Whisper is installed)
   */
  def initialize(): Try[Unit] = Try {
    println(s"Checking for Whisper installation...")
    
    // Check if whisper is installed
    val checkCmd = Seq("python3", "-c", "import whisper; print('OK')")
    val result = checkCmd.!!.trim
    
    if result != "OK" then
      throw RuntimeException(
        "Whisper not installed. Install with: pip install openai-whisper"
      )
    
    println(s"✓ Whisper (model: $modelName) is available")
    println("Note: Model will be downloaded automatically on first use")
  }

  /**
   * Transcribe audio data in real-time
   * @param audioData Raw audio bytes (16kHz, 16-bit, mono PCM)
   * @param detectLanguage Whether to detect language automatically
   * @return Transcribed text and detected language
   */
  def transcribe(
    audioData: Array[Byte],
    detectLanguage: Boolean = true,
    language: Option[String] = None
  ): Try[TranscriptionResult] = Try {
    whisperContext match
      case Some(ctx) =>
        // Convert bytes to float array (Whisper expects normalized float samples)
        val samples = bytesToFloatSamples(audioData)

        if samples.isEmpty then
          return Success(TranscriptionResult("", None, 0))

        // Create full parameters
        val params = WhisperFullParams()

        // Configure parameters for real-time transcription
        params.strategy = WhisperFullParams.WHISPER_SAMPLING_GREEDY
        params.printProgress = false
        params.printRealtime = false
        params.printTimestamps = false

        // Language detection or use specified language
        if detectLanguage then
          params.language = "auto"
        else
          language.foreach(lang => params.language = lang)

        // Enable translation to English if needed
        params.translate = false

        // Process the audio
        val result = whisper.full(ctx, params, samples, samples.length)

        if result != 0 then
          throw RuntimeException(s"Whisper transcription failed with code: $result")

        // Get the transcription
        val segmentCount = whisper.fullNSegments(ctx)
        val text = StringBuilder()

        for i <- 0 until segmentCount do
          val segmentText = whisper.fullGetSegmentText(ctx, i)
          text.append(segmentText)

        // Detect language
        val detectedLang = if detectLanguage then
          Some(whisper.fullLangStr(ctx))
        else
          language

        TranscriptionResult(
          text = text.toString.trim,
          language = detectedLang,
          confidence = 0.0f // WhisperJNI doesn't provide confidence scores directly
        )

      case None =>
        throw IllegalStateException("Whisper model not initialized. Call initialize() first.")
  }

  /**
   * Convert byte array (16-bit PCM) to float array (normalized -1.0 to 1.0)
   */
  private def bytesToFloatSamples(bytes: Array[Byte]): Array[Float] =
    if bytes.length < 2 then return Array.empty[Float]

    val buffer = ByteBuffer.wrap(bytes)
    buffer.order(ByteOrder.LITTLE_ENDIAN)

    val samples = new Array[Float](bytes.length / 2)
    var i = 0

    while buffer.remaining() >= 2 do
      val shortValue = buffer.getShort()
      samples(i) = shortValue.toFloat / 32768.0f
      i += 1

    samples.take(i)

  /**
   * Release resources
   */
  def close(): Unit =
    whisperContext.foreach { ctx =>
      whisper.freeContext(ctx)
      println("✓ Whisper model released")
    }
    whisperContext = None

/**
 * Result of transcription
 */
case class TranscriptionResult(
  text: String,
  language: Option[String],
  confidence: Float
)
