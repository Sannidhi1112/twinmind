package com.twinmind.voicerecorder.data.repository

import android.util.Log
import com.google.gson.Gson
import com.twinmind.voicerecorder.data.local.entity.Summary
import com.twinmind.voicerecorder.data.local.entity.SummaryStatus
import com.twinmind.voicerecorder.data.remote.api.MockApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
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

    suspend fun generateSummary(meetingId: Long): Flow<SummaryProgress> = flow {
        try {
            Log.d(TAG, "Starting summary generation for meeting $meetingId")

            // Emit initial state
            emit(SummaryProgress.Loading("Preparing to generate summary..."))

            // Get all transcripts
            val chunks = meetingRepository.getChunksByMeetingId(meetingId)
            val transcripts = chunks.mapNotNull { it.transcriptText }

            if (transcripts.isEmpty()) {
                emit(SummaryProgress.Error("No transcripts available for summary generation"))
                return@flow
            }

            val fullTranscript = transcripts.joinToString(" ")

            // Update status to in progress
            meetingRepository.updateSummaryStatus(meetingId, SummaryStatus.IN_PROGRESS)

            // Check if summary already exists
            var summary = meetingRepository.getSummaryByMeetingId(meetingId)
            if (summary == null) {
                // Create new summary entry
                val summaryId = meetingRepository.createSummary(
                    Summary(
                        meetingId = meetingId,
                        fullTranscript = fullTranscript,
                        status = SummaryStatus.IN_PROGRESS
                    )
                )
                summary = meetingRepository.getSummaryByMeetingId(meetingId)
            }

            emit(SummaryProgress.Loading("Generating summary from transcript..."))

            // Generate summary using API (currently mock)
            val result = withContext(Dispatchers.IO) {
                mockApiService.generateSummary(fullTranscript)
            }

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

            // Update meeting status
            meetingRepository.updateSummaryStatus(meetingId, SummaryStatus.COMPLETED)

            // Emit success with final summary
            emit(
                SummaryProgress.Success(
                    title = result.title,
                    summary = result.summary,
                    actionItems = result.actionItems,
                    keyPoints = result.keyPoints
                )
            )

            Log.d(TAG, "Successfully generated summary for meeting $meetingId")
        } catch (e: Exception) {
            Log.e(TAG, "Error generating summary for meeting $meetingId", e)

            // Update status to failed
            meetingRepository.updateSummaryStatusOnly(
                meetingId,
                SummaryStatus.FAILED,
                e.message
            )

            emit(SummaryProgress.Error(e.message ?: "Failed to generate summary"))
        }
    }

    suspend fun getSummaryData(meetingId: Long): SummaryData? = withContext(Dispatchers.IO) {
        try {
            val summary = meetingRepository.getSummaryByMeetingId(meetingId) ?: return@withContext null

            val actionItems = try {
                gson.fromJson(summary.actionItems, Array<String>::class.java).toList()
            } catch (e: Exception) {
                emptyList()
            }

            val keyPoints = try {
                gson.fromJson(summary.keyPoints, Array<String>::class.java).toList()
            } catch (e: Exception) {
                emptyList()
            }

            SummaryData(
                title = summary.title,
                summary = summary.summary,
                actionItems = actionItems,
                keyPoints = keyPoints,
                status = summary.status,
                errorMessage = summary.errorMessage
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getting summary data", e)
            null
        }
    }

    sealed class SummaryProgress {
        data class Loading(val message: String) : SummaryProgress()
        data class Success(
            val title: String,
            val summary: String,
            val actionItems: List<String>,
            val keyPoints: List<String>
        ) : SummaryProgress()
        data class Error(val message: String) : SummaryProgress()
    }

    data class SummaryData(
        val title: String,
        val summary: String,
        val actionItems: List<String>,
        val keyPoints: List<String>,
        val status: SummaryStatus,
        val errorMessage: String?
    )
}
