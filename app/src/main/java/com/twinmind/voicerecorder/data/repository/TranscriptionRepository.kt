package com.twinmind.voicerecorder.data.repository

import android.util.Log
import com.twinmind.voicerecorder.data.local.entity.TranscriptStatus
import com.twinmind.voicerecorder.data.remote.api.MockApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TranscriptionRepository @Inject constructor(
    private val mockApiService: MockApiService,
    private val meetingRepository: MeetingRepository
) {

    companion object {
        private const val TAG = "TranscriptionRepository"
        private const val MAX_RETRIES = 3
    }

    suspend fun transcribeChunk(
        chunkId: Long,
        audioFile: File,
        chunkNumber: Int
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting transcription for chunk $chunkId (number: $chunkNumber)")

            // Check if file exists
            if (!audioFile.exists()) {
                return@withContext Result.failure(Exception("Audio file not found: ${audioFile.path}"))
            }

            // Update status to transcribing
            meetingRepository.updateChunkTranscript(
                chunkId,
                TranscriptStatus.IN_PROGRESS,
                null
            )

            // Call transcription API (currently using mock)
            val transcriptText = mockApiService.transcribeAudio(audioFile, chunkNumber)

            // Update chunk with transcript
            meetingRepository.updateChunkTranscript(
                chunkId,
                TranscriptStatus.COMPLETED,
                transcriptText
            )

            Log.d(TAG, "Successfully transcribed chunk $chunkId")
            Result.success(transcriptText)
        } catch (e: Exception) {
            Log.e(TAG, "Error transcribing chunk $chunkId", e)

            // Update chunk with error
            meetingRepository.updateChunkTranscriptError(
                chunkId,
                TranscriptStatus.FAILED,
                e.message ?: "Unknown error"
            )

            Result.failure(e)
        }
    }

    suspend fun transcribeAllChunks(meetingId: Long): Result<String> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting transcription for all chunks of meeting $meetingId")

            // Update meeting transcript status
            meetingRepository.updateTranscriptStatus(meetingId, TranscriptStatus.IN_PROGRESS)

            // Get all chunks that need transcription
            val chunks = meetingRepository.getChunksByMeetingId(meetingId)
            if (chunks.isEmpty()) {
                return@withContext Result.failure(Exception("No audio chunks found for meeting"))
            }

            val transcriptParts = mutableListOf<String>()
            var successCount = 0
            var failureCount = 0

            // Transcribe each chunk
            for (chunk in chunks) {
                if (chunk.transcriptStatus == TranscriptStatus.COMPLETED) {
                    // Already transcribed
                    chunk.transcriptText?.let { transcriptParts.add(it) }
                    successCount++
                    continue
                }

                val audioFile = File(chunk.filePath)
                var retryCount = 0
                var success = false

                while (retryCount < MAX_RETRIES && !success) {
                    val result = transcribeChunk(chunk.id, audioFile, chunk.chunkNumber)
                    if (result.isSuccess) {
                        result.getOrNull()?.let { transcriptParts.add(it) }
                        successCount++
                        success = true
                    } else {
                        retryCount++
                        Log.w(TAG, "Retry $retryCount/$MAX_RETRIES for chunk ${chunk.id}")
                        if (retryCount >= MAX_RETRIES) {
                            failureCount++
                        }
                    }
                }

                // Update progress
                meetingRepository.updateTranscribedChunks(meetingId, successCount)
            }

            // Check if all chunks were transcribed
            if (failureCount > 0) {
                meetingRepository.updateTranscriptStatus(meetingId, TranscriptStatus.FAILED)
                return@withContext Result.failure(
                    Exception("Failed to transcribe $failureCount out of ${chunks.size} chunks")
                )
            }

            // Combine all transcripts
            val fullTranscript = transcriptParts.joinToString(" ")

            // Update meeting status
            meetingRepository.updateTranscriptStatus(meetingId, TranscriptStatus.COMPLETED)

            Log.d(TAG, "Successfully transcribed all chunks for meeting $meetingId")
            Result.success(fullTranscript)
        } catch (e: Exception) {
            Log.e(TAG, "Error transcribing chunks for meeting $meetingId", e)
            meetingRepository.updateTranscriptStatus(meetingId, TranscriptStatus.FAILED)
            Result.failure(e)
        }
    }

    suspend fun retryFailedChunks(meetingId: Long): Result<String> = withContext(Dispatchers.IO) {
        try {
            val failedChunks = meetingRepository.getChunksByTranscriptStatus(
                meetingId,
                TranscriptStatus.FAILED
            )

            if (failedChunks.isEmpty()) {
                return@withContext Result.success("No failed chunks to retry")
            }

            Log.d(TAG, "Retrying ${failedChunks.size} failed chunks for meeting $meetingId")

            var successCount = 0
            for (chunk in failedChunks) {
                val audioFile = File(chunk.filePath)
                val result = transcribeChunk(chunk.id, audioFile, chunk.chunkNumber)
                if (result.isSuccess) {
                    successCount++
                }
            }

            if (successCount == failedChunks.size) {
                Result.success("Successfully retried all failed chunks")
            } else {
                Result.failure(Exception("Retried $successCount out of ${failedChunks.size} chunks"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error retrying failed chunks", e)
            Result.failure(e)
        }
    }
}
