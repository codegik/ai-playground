package com.codegik.transcript.transcription

import scala.util.{Try, Success}
import scala.sys.process.*
import java.nio.file.Files
import java.io.File
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
    if audioData.isEmpty then
      TranscriptionResult("", None, 0.0f)
    else
      // Save audio to temporary WAV file
      val tempFile = File.createTempFile("audio-", ".wav", tempDir)
      saveAsWav(audioData, tempFile)

      try {
        // Build Whisper command
        val languageParam = if detectLanguage then "" else language.map(l => s"--language $l").getOrElse("")
        val cmd = s"python3 -c \"import whisper; import json; model = whisper.load_model('$modelName'); result = model.transcribe('${tempFile.getAbsolutePath}' $languageParam); print(json.dumps({'text': result['text'], 'language': result['language']}))\""

        // Execute Whisper
        val output = cmd.!!.trim

        // Parse JSON output
        parseWhisperOutput(output)
      } finally {
        tempFile.delete()
      }
  }

  /**
   * Save audio bytes as WAV file
   */
  private def saveAsWav(audioData: Array[Byte], file: File): Unit =
    val audioFormat = AudioFormat(
      16000.0f,  // sample rate
      16,        // sample size in bits
      1,         // channels (mono)
      true,      // signed
      false      // little endian
    )

    val audioInputStream = AudioInputStream(
      ByteArrayInputStream(audioData),
      audioFormat,
      audioData.length / audioFormat.getFrameSize
    )

    AudioSystem.write(
      audioInputStream,
      AudioFileFormat.Type.WAVE,
      file
    )

  /**
   * Parse Whisper Python output
   */
  private def parseWhisperOutput(jsonOutput: String): TranscriptionResult =
    // Simple JSON parsing (avoiding heavy dependencies)
    val textPattern = """"text":\s*"([^"]*)"""".r
    val langPattern = """"language":\s*"([^"]*)"""".r

    val text = textPattern.findFirstMatchIn(jsonOutput)
      .map(_.group(1).trim)
      .getOrElse("")

    val lang = langPattern.findFirstMatchIn(jsonOutput)
      .map(_.group(1))
      .orElse(Some("unknown"))

    TranscriptionResult(text, lang, 1.0f)

  /**
   * Release resources
   */
  def close(): Unit =
    // Clean up temp directory
    tempDir.listFiles().foreach(_.delete())
    tempDir.delete()
    println("✓ Whisper resources released")

/**
 * Result of transcription
 */
case class TranscriptionResult(
  text: String,
  language: Option[String],
  confidence: Float
)
