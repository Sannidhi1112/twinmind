package com.voicerecorder.service

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.voicerecorder.data.repository.SummaryRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.collectLatest

@HiltWorker
class SummaryWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val summaryRepository: SummaryRepository
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "SummaryWorker"
    }

    override suspend fun doWork(): Result {
        val meetingId = inputData.getLong("meeting_id", -1L)

        if (meetingId == -1L) {
            Log.e(TAG, "Invalid meeting_id")
            return Result.failure()
        }

        return try {
            Log.d(TAG, "Starting summary generation for meeting $meetingId")

            var result: Result = Result.failure()

            summaryRepository.generateSummary(meetingId).collectLatest { progress ->
                when (progress) {
                    is SummaryRepository.SummaryProgress.Loading -> {
                        Log.d(TAG, "Summary generation in progress: ${progress.message}")
                    }
                    is SummaryRepository.SummaryProgress.Streaming -> {
                        Log.d(TAG, "Summary generation streaming: ${progress.partialSummary}")
                    }
                    is SummaryRepository.SummaryProgress.Success -> {
                        Log.d(TAG, "Successfully generated summary for meeting $meetingId")
                        result = Result.success()
                    }
                    is SummaryRepository.SummaryProgress.Error -> {
                        Log.e(TAG, "Failed to generate summary: ${progress.message}")
                        result = Result.failure()
                    }
                }
            }

            result
        } catch (e: Exception) {
            Log.e(TAG, "Error in summary worker", e)
            Result.failure()
        }
    }
}
