package com.twinmind.voicerecorder.utils

import android.content.Context
import android.media.MediaRecorder
import android.os.Build

object AudioUtils {

    const val CHUNK_DURATION_MS = 30_000L // 30 seconds
    const val OVERLAP_DURATION_MS = 2_000L // 2 seconds overlap
    const val SAMPLE_RATE = 44100
    const val BIT_RATE = 128000

    const val SILENCE_THRESHOLD = 500 // Amplitude threshold for silence detection
    const val SILENCE_DETECTION_DURATION_MS = 10_000L // 10 seconds

    fun getAudioSource(): Int {
        return MediaRecorder.AudioSource.MIC
    }

    fun getOutputFormat(): Int {
        return MediaRecorder.OutputFormat.MPEG_4
    }

    fun getAudioEncoder(): Int {
        return MediaRecorder.AudioEncoder.AAC
    }

    fun formatDuration(durationMs: Long): String {
        val seconds = (durationMs / 1000) % 60
        val minutes = (durationMs / (1000 * 60)) % 60
        val hours = (durationMs / (1000 * 60 * 60))

        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }

    fun isAudioSourceAvailable(context: Context, audioSource: Int): Boolean {
        return try {
            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            recorder.setAudioSource(audioSource)
            recorder.release()
            true
        } catch (e: Exception) {
            false
        }
    }
}
