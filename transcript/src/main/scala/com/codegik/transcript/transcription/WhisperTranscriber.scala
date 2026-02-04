package com.codegik.transcript.transcription

import scala.util.Try
import java.nio.file.{Files, Paths}
import org.vosk.{Model, Recognizer}
import com.google.gson.JsonParser

/**
 * Real-time audio transcription using Vosk
 * Much faster than Whisper for real-time streaming!
 * Requires: Download Vosk model
 */
class WhisperTranscriber(modelPath: String = "models/vosk-model-small-en-us-0.15"):

  private var model: Option[Model] = None
  private var recognizer: Option[Recognizer] = None

  /**
   * Initialize the Vosk model
   */
  def initialize(): Try[Unit] = Try {
    println(s"Loading Vosk model from: $modelPath")

    if !Files.exists(Paths.get(modelPath)) then
      throw RuntimeException(
        s"Model not found at: $modelPath\n" +
        "Download a model from: https://alphacephei.com/vosk/models\n" +
        "Example:\n" +
        "  mkdir -p models\n" +
        "  cd models\n" +
        "  wget https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip\n" +
        "  unzip vosk-model-small-en-us-0.15.zip"
      )

    // Load model (this is done once and reused for all transcriptions!)
    val voskModel = new Model(modelPath)
    model = Some(voskModel)

    // Create recognizer for 16kHz audio
    val voskRecognizer = new Recognizer(voskModel, 16000)
    recognizer = Some(voskRecognizer)

    println(s"✓ Vosk model loaded successfully")
    println("✓ Ready for real-time transcription!")
  }

  /**
   * Transcribe audio data in real-time
   * @param audioData Raw audio bytes (16kHz, 16-bit, mono PCM)
   * @param detectLanguage Whether to detect language automatically (not used in Vosk)
   * @return Transcribed text and detected language
   */
  def transcribe(
    audioData: Array[Byte],
    detectLanguage: Boolean = true,
    language: Option[String] = None
  ): Try[TranscriptionResult] = Try {
    recognizer match
      case Some(rec) =>
        if audioData.isEmpty then
          TranscriptionResult("", Some("en"), 0.0f)
        else
          // Feed audio data to recognizer
          val accepted = rec.acceptWaveForm(audioData, audioData.length)

          // Get final result (completed text) instead of partial to avoid accumulation
          val resultJson = rec.getFinalResult()

          // Parse JSON result
          val result = parseVoskResult(resultJson)
          
          // Reset recognizer for next chunk to prevent accumulation
          if result.text.nonEmpty then
            rec.reset()
          
          result

      case None =>
        throw IllegalStateException("Vosk model not initialized. Call initialize() first.")
  }

  /**
   * Get final result (call this when audio stream ends)
   */
  def getFinalResult(): TranscriptionResult =
    recognizer match
      case Some(rec) =>
        val resultJson = rec.getFinalResult()
        parseVoskResult(resultJson)
      case None =>
        TranscriptionResult("", Some("en"), 0.0f)

  /**
   * Parse Vosk JSON result
   */
  private def parseVoskResult(jsonResult: String): TranscriptionResult =
    try {
      val parser = JsonParser.parseString(jsonResult)
      val jsonObject = parser.getAsJsonObject

      val text = if jsonObject.has("partial") then
        jsonObject.get("partial").getAsString
      else if jsonObject.has("text") then
        jsonObject.get("text").getAsString
      else
        ""

      TranscriptionResult(text.trim, Some("en"), 1.0f)
    } catch {
      case e: Exception =>
        TranscriptionResult("", Some("en"), 0.0f)
    }

  /**
   * Reset recognizer (for starting fresh)
   */
  def reset(): Unit =
    recognizer.foreach(_.reset())

  /**
   * Release resources
   */
  def close(): Unit =
    recognizer.foreach { rec =>
      rec.close()
    }
    model.foreach { m =>
      m.close()
    }
    recognizer = None
    model = None
    println("✓ Vosk model released")

/**
 * Result of transcription
 */
case class TranscriptionResult(
  text: String,
  language: Option[String],
  confidence: Float
)
