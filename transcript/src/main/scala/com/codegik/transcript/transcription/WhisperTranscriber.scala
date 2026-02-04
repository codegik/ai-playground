package com.codegik.transcript.transcription

import scala.util.{Try, Using}
import java.nio.file.Files
import java.io.{File, BufferedReader, InputStreamReader, PrintWriter}
import javax.sound.sampled.{AudioFileFormat, AudioFormat, AudioInputStream, AudioSystem}
import java.io.ByteArrayInputStream

/**
 * Real-time audio transcription using Whisper via a PERSISTENT Python process
 * This keeps the model loaded in memory, making it 10-20x faster!
 */
class WhisperTranscriber(modelName: String = "tiny"):

  private val tempDir = Files.createTempDirectory("whisper-transcript").toFile
  tempDir.deleteOnExit()

  private var whisperProcess: Option[Process] = None
  private var processInput: Option[PrintWriter] = None
  private var processOutput: Option[BufferedReader] = None

  /**
   * Initialize the Whisper model by starting a persistent Python process
   */
  def initialize(): Try[Unit] = Try {
    println(s"Starting Whisper server (model: $modelName)...")
    println("This loads the model ONCE and keeps it in memory for speed!")

    // Create the Python script
    val scriptFile = new File(tempDir, "whisper_server.py")
    val scriptContent = s"""#!/usr/bin/env python3
import sys
import json
import whisper
import warnings
warnings.filterwarnings("ignore")

print("Loading Whisper model: $modelName...", file=sys.stderr, flush=True)
model = whisper.load_model("$modelName")
print("✓ Model loaded and ready!", file=sys.stderr, flush=True)
print("READY", flush=True)

while True:
    try:
        line = sys.stdin.readline()
        if not line:
            break

        audio_path = line.strip()
        if not audio_path or audio_path == "QUIT":
            break

        result = model.transcribe(audio_path, fp16=False)
        output = {"text": result["text"], "language": result["language"]}
        print(json.dumps(output), flush=True)

    except Exception as e:
        output = {"text": "", "language": "unknown", "error": str(e)}
        print(json.dumps(output), flush=True)
"""

    Files.writeString(scriptFile.toPath, scriptContent)
    scriptFile.setExecutable(true)

    // Start the Python process
    val processBuilder = new ProcessBuilder("python3", scriptFile.getAbsolutePath)
    processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT)

    val process = processBuilder.start()
    whisperProcess = Some(process)

    // Setup I/O streams
    processInput = Some(new PrintWriter(process.getOutputStream, true))
    processOutput = Some(new BufferedReader(new InputStreamReader(process.getInputStream)))

    // Wait for READY signal
    val ready = processOutput.get.readLine()
    if ready != "READY" then
      throw RuntimeException(s"Whisper server failed to start. Got: $ready")

    println(s"✓ Whisper server ready! (Model loaded once in memory)")
  }

  /**
   * Transcribe audio data using the persistent process
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
        // Send file path to Python process
        processInput.get.println(tempFile.getAbsolutePath)

        // Read response
        val response = processOutput.get.readLine()
        if response == null then
          throw RuntimeException("Whisper server stopped responding")

        // Parse JSON response
        parseWhisperOutput(response)
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
   * Parse Whisper JSON output
   */
  private def parseWhisperOutput(jsonOutput: String): TranscriptionResult =
    val textPattern = """"text":\s*"([^"]*)"""".r
    val langPattern = """"language":\s*"([^"]*)"""".r

    val text = textPattern.findFirstMatchIn(jsonOutput)
      .map(_.group(1).trim)
      .getOrElse("")

    val lang = langPattern.findFirstMatchIn(jsonOutput)
      .map(_.group(1))

    TranscriptionResult(text, lang, 1.0f)

  /**
   * Release resources and stop the Python process
   */
  def close(): Unit =
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

    whisperProcess = None
    processInput = None
    processOutput = None

    // Clean up temp directory
    tempDir.listFiles().foreach(_.delete())
    tempDir.delete()

    println("✓ Whisper server stopped")

/**
 * Result of transcription
 */
case class TranscriptionResult(
  text: String,
  language: Option[String],
  confidence: Float
)
