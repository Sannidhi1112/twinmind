package com.voicerecorder.data.repository

import android.util.Log
import com.google.gson.Gson
import com.voicerecorder.data.local.entity.SummaryStatus
import com.voicerecorder.data.remote.api.MockApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SummaryRepository @Inject constructor(
    private val mockApiService: MockApiService,
    private val meetingRepository: MeetingRepository,
    private val gson: Gson
) {

    companion object {
        private const val TAG = "SummaryRepository"
    }

    fun generateSummary(meetingId: Long): Flow<SummaryProgress> = flow {
        try {
            Log.d(TAG, "Starting summary generation for meeting $meetingId")
            emit(SummaryProgress.Loading("Preparing summary..."))

            // Get transcript from database
            val chunks = meetingRepository.getChunksByMeetingId(meetingId)
            val fullTranscript = chunks.joinToString(" ") { it.transcriptText ?: "" }

            if (fullTranscript.isBlank()) {
                emit(SummaryProgress.Error("Transcript is empty, cannot generate summary."))
                return@flow
            }

            // Update status in database
            meetingRepository.updateSummaryStatus(meetingId, SummaryStatus.IN_PROGRESS)

            // Generate summary using API (currently mock)
            val result = mockApiService.generateSummary()

            // Convert action items and key points to JSON
            val actionItemsJson = gson.toJson(result.actionItems)
            val keyPointsJson = gson.toJson(result.keyPoints)

            // Update summary in database
            meetingRepository.updateSummaryContent(
                meetingId = meetingId,
                title = result.title,
                summary = result.summary,
                actionItems = actionItemsJson,
                keyPoints = keyPointsJson,
                status = SummaryStatus.COMPLETED
            )

            emit(SummaryProgress.Success(result.title, result.summary, result.actionItems, result.keyPoints))
            Log.d(TAG, "Successfully generated summary for meeting $meetingId")
        } catch (e: Exception) {
            Log.e(TAG, "Error generating summary for meeting $meetingId", e)
            meetingRepository.updateSummaryStatusOnly(meetingId, SummaryStatus.FAILED, e.message)
            emit(SummaryProgress.Error(e.message ?: "Unknown error"))
        }
    }.flowOn(Dispatchers.IO)

    sealed class SummaryProgress {
        data class Loading(val message: String) : SummaryProgress()
        data class Streaming(val partialSummary: String) : SummaryProgress()
        data class Success(
            val title: String,
            val summary: String,
            val actionItems: List<String>,
            val keyPoints: List<String>
        ) : SummaryProgress()
        data class Error(val message: String) : SummaryProgress()
    }
}
