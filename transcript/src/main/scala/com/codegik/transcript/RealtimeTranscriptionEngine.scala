package com.codegik.transcript

import com.codegik.transcript.audio.MicrophoneCapture
import com.codegik.transcript.transcription.{WhisperTranscriber, TranscriptionResult}
import scala.util.{Try, Success, Failure}
import scala.concurrent.{Future, ExecutionContext, Await}
import scala.concurrent.duration.*
import java.util.concurrent.Executors
import java.io.ByteArrayOutputStream

/**
 * Real-time audio transcription engine
 * Continuously captures audio and transcribes it in real-time
 */
class RealtimeTranscriptionEngine(
  modelPath: String,
  chunkDurationMs: Int = 2000, // Process audio in 2-second chunks
  silenceThreshold: Float = 0.01f
)(using ExecutionContext):

  private val microphone = MicrophoneCapture()
  private val transcriber = WhisperTranscriber(modelPath)
  private var isRunning = false
  private var transcriptionCallback: Option[TranscriptionResult => Unit] = None

  /**
   * Initialize the transcription engine
   */
  def initialize(): Try[Unit] =
    for
      _ <- transcriber.initialize()
      _ <- microphone.open()
    yield ()

  /**
   * Start real-time transcription
   * @param callback Function to call with each transcription result
   */
  def start(callback: TranscriptionResult => Unit): Try[Unit] = Try {
    if isRunning then
      throw IllegalStateException("Transcription already running")

    transcriptionCallback = Some(callback)
    microphone.start()
    isRunning = true

    println(s"✓ Real-time transcription started (processing ${chunkDurationMs}ms chunks)")
    println("Speak into your microphone...")
    println("-" * 60)

    // Start the transcription loop
    transcriptionLoop()
  }

  /**
   * Main transcription loop
   */
  private def transcriptionLoop(): Unit =
    Future {
      val audioBuffer = ByteArrayOutputStream()

      while isRunning do
        try
          // Read audio chunk
          val audioChunk = microphone.readAudioForDuration(chunkDurationMs)

          if audioChunk.nonEmpty && !isSilence(audioChunk) then
            // Transcribe the audio chunk
            transcriber.transcribe(audioChunk) match
              case Success(result) if result.text.nonEmpty =>
                transcriptionCallback.foreach(_(result))

              case Success(_) =>
                // Empty transcription, continue

              case Failure(exception) =>
                System.err.println(s"Transcription error: ${exception.getMessage}")

          // Small delay to prevent CPU overload
          Thread.sleep(100)

        catch
          case e: InterruptedException =>
            isRunning = false
          case e: Exception =>
            System.err.println(s"Error in transcription loop: ${e.getMessage}")
    }

  /**
   * Check if audio chunk is mostly silence
   */
  private def isSilence(audioData: Array[Byte]): Boolean =
    if audioData.isEmpty then return true

    var sum = 0.0
    var i = 0
    while i < audioData.length - 1 do
      val sample = ((audioData(i + 1) << 8) | (audioData(i) & 0xFF)).toShort
      sum += Math.abs(sample.toFloat / 32768.0f)
      i += 2

    val average = sum / (audioData.length / 2)
    average < silenceThreshold

  /**
   * Stop transcription
   */
  def stop(): Unit =
    if isRunning then
      isRunning = false
      microphone.stop()
      println("\n✓ Real-time transcription stopped")

  /**
   * Release all resources
   */
  def close(): Unit =
    stop()
    microphone.close()
    transcriber.close()
    println("✓ Transcription engine closed")
