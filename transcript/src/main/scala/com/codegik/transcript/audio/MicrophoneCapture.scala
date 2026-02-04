package com.codegik.transcript.audio

import javax.sound.sampled.*
import java.io.ByteArrayOutputStream
import scala.util.{Try, Success, Failure}

/**
 * Captures audio from the system microphone in real-time
 * Outputs 16kHz, 16-bit, mono PCM audio (Whisper's expected format)
 */
class MicrophoneCapture(
  val sampleRate: Float = 16000.0f,
  val sampleSizeInBits: Int = 16,
  val channels: Int = 1,
  val signed: Boolean = true,
  val bigEndian: Boolean = false
):

  private val audioFormat = AudioFormat(
    sampleRate,
    sampleSizeInBits,
    channels,
    signed,
    bigEndian
  )

  private var targetDataLine: Option[TargetDataLine] = None
  private var isCapturing = false

  /**
   * Initialize and open the microphone
   */
  def open(): Try[Unit] = Try {
    val dataLineInfo = DataLine.Info(classOf[TargetDataLine], audioFormat)

    if !AudioSystem.isLineSupported(dataLineInfo) then
      throw RuntimeException("Microphone not supported with the specified audio format")

    val line = AudioSystem.getLine(dataLineInfo).asInstanceOf[TargetDataLine]
    line.open(audioFormat)
    targetDataLine = Some(line)
    println(s"✓ Microphone opened: ${sampleRate}Hz, ${sampleSizeInBits}-bit, ${channels} channel(s)")
  }

  /**
   * Start capturing audio from the microphone
   */
  def start(): Try[Unit] = Try {
    targetDataLine match
      case Some(line) =>
        line.start()
        isCapturing = true
        println("✓ Microphone capture started")
      case None =>
        throw IllegalStateException("Microphone not opened. Call open() first.")
  }

  /**
   * Read audio data from the microphone
   * @param bufferSize Size of the buffer to read
   * @return Array of audio bytes
   */
  def readAudio(bufferSize: Int = 4096): Option[Array[Byte]] =
    targetDataLine match
      case Some(line) if isCapturing =>
        val buffer = new Array[Byte](bufferSize)
        val bytesRead = line.read(buffer, 0, buffer.length)
        if bytesRead > 0 then Some(buffer.take(bytesRead))
        else None
      case _ => None

  /**
   * Read audio data for a specific duration
   * @param durationMs Duration in milliseconds
   * @return Array of audio bytes
   */
  def readAudioForDuration(durationMs: Int): Array[Byte] =
    val bytesPerMs = (sampleRate * (sampleSizeInBits / 8) * channels / 1000).toInt
    val totalBytes = bytesPerMs * durationMs
    val buffer = new Array[Byte](totalBytes)

    targetDataLine match
      case Some(line) if isCapturing =>
        var totalRead = 0
        while totalRead < totalBytes do
          val bytesRead = line.read(buffer, totalRead, totalBytes - totalRead)
          if bytesRead > 0 then totalRead += bytesRead
        buffer
      case _ => Array.empty[Byte]

  /**
   * Stop capturing audio
   */
  def stop(): Unit =
    targetDataLine.foreach { line =>
      isCapturing = false
      line.stop()
      println("✓ Microphone capture stopped")
    }

  /**
   * Close the microphone and release resources
   */
  def close(): Unit =
    stop()
    targetDataLine.foreach { line =>
      line.close()
      println("✓ Microphone closed")
    }
    targetDataLine = None

  /**
   * Get the audio format
   */
  def getAudioFormat: AudioFormat = audioFormat

  /**
   * Check if currently capturing
   */
  def isActive: Boolean = isCapturing
