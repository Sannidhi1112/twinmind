package com.voicerecorder.data.local.dao

import androidx.room.*
import com.voicerecorder.data.local.entity.Summary
import com.voicerecorder.data.local.entity.SummaryStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface SummaryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(summary: Summary): Long

    @Update
    suspend fun update(summary: Summary)

    @Delete
    suspend fun delete(summary: Summary)

    @Query("SELECT * FROM summaries WHERE id = :summaryId")
    suspend fun getSummaryById(summaryId: Long): Summary?

    @Query("SELECT * FROM summaries WHERE meetingId = :meetingId")
    suspend fun getSummaryByMeetingId(meetingId: Long): Summary?

    @Query("SELECT * FROM summaries WHERE meetingId = :meetingId")
    fun getSummaryByMeetingIdFlow(meetingId: Long): Flow<Summary?>

    @Query("UPDATE summaries SET status = :status, errorMessage = :errorMessage, updatedAt = :updatedAt WHERE meetingId = :meetingId")
    suspend fun updateSummaryStatus(
        meetingId: Long,
        status: SummaryStatus,
        errorMessage: String? = null,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("UPDATE summaries SET title = :title, summary = :summary, actionItems = :actionItems, keyPoints = :keyPoints, status = :status, updatedAt = :updatedAt WHERE meetingId = :meetingId")
    suspend fun updateSummaryContent(
        meetingId: Long,
        title: String,
        summary: String,
        actionItems: String,
        keyPoints: String,
        status: SummaryStatus = SummaryStatus.COMPLETED,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("DELETE FROM summaries WHERE meetingId = :meetingId")
    suspend fun deleteSummaryByMeetingId(meetingId: Long)
}
