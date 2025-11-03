package com.voicerecorder.service

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.voicerecorder.data.repository.MeetingRepository
import com.voicerecorder.data.repository.TranscriptionRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.File

@HiltWorker
class TranscriptionWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val transcriptionRepository: TranscriptionRepository,
    private val meetingRepository: MeetingRepository
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "TranscriptionWorker"
    }

    override suspend fun doWork(): Result {
        val chunkId = inputData.getLong("chunk_id", -1L)
        val meetingId = inputData.getLong("meeting_id", -1L)

        if (chunkId == -1L || meetingId == -1L) {
            Log.e(TAG, "Invalid chunk_id or meeting_id")
            return Result.failure()
        }

        return try {
            Log.d(TAG, "Starting transcription for chunk $chunkId")

            val chunks = meetingRepository.getChunksByMeetingId(meetingId)
            val chunk = chunks.find { it.id == chunkId }

            if (chunk == null) {
                Log.e(TAG, "Chunk not found: $chunkId")
                return Result.failure()
            }

            val audioFile = File(chunk.filePath)
            if (!audioFile.exists()) {
                Log.e(TAG, "Audio file not found: ${chunk.filePath}")
                return Result.failure()
            }

            val result = transcriptionRepository.transcribeChunk(
                chunkId,
                audioFile,
                chunk.chunkNumber
            )

            if (result.isSuccess) {
                Log.d(TAG, "Successfully transcribed chunk $chunkId")

                // **Fix: Update the transcribed chunk count**
                val transcribedChunks = meetingRepository.getTranscribedChunkCount(meetingId)
                meetingRepository.updateTranscribedChunks(meetingId, transcribedChunks)

                val totalChunks = meetingRepository.getChunkCount(meetingId)
                if (transcribedChunks >= totalChunks) {
                    Log.d(TAG, "All chunks transcribed for meeting $meetingId, triggering summary generation")
                    triggerSummaryGeneration(meetingId)
                }

                Result.success()
            } else {
                Log.e(TAG, "Failed to transcribe chunk $chunkId: ${result.exceptionOrNull()?.message}")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in transcription worker", e)
            Result.failure()
        }
    }

    private fun triggerSummaryGeneration(meetingId: Long) {
        val workManager = androidx.work.WorkManager.getInstance(applicationContext)
        val workRequest = androidx.work.OneTimeWorkRequestBuilder<SummaryWorker>()
            .setInputData(androidx.work.workDataOf("meeting_id" to meetingId))
            .build()

        workManager.enqueue(workRequest)
    }
}
