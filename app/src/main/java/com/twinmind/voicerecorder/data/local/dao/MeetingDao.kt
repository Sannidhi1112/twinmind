package com.twinmind.voicerecorder.data.local.dao

import androidx.room.*
import com.twinmind.voicerecorder.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MeetingDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(meeting: Meeting): Long

    @Update
    suspend fun update(meeting: Meeting)

    @Delete
    suspend fun delete(meeting: Meeting)

    @Query("SELECT * FROM meetings WHERE id = :meetingId")
    suspend fun getMeetingById(meetingId: Long): Meeting?

    @Query("SELECT * FROM meetings WHERE id = :meetingId")
    fun getMeetingByIdFlow(meetingId: Long): Flow<Meeting?>

    @Query("SELECT * FROM meetings ORDER BY startTime DESC")
    fun getAllMeetings(): Flow<List<Meeting>>

    @Query("SELECT * FROM meetings WHERE status = :status")
    fun getMeetingsByStatus(status: MeetingStatus): Flow<List<Meeting>>

    @Query("UPDATE meetings SET status = :status, updatedAt = :updatedAt WHERE id = :meetingId")
    suspend fun updateMeetingStatus(meetingId: Long, status: MeetingStatus, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE meetings SET recordingState = :state, pauseReason = :pauseReason, updatedAt = :updatedAt WHERE id = :meetingId")
    suspend fun updateRecordingState(
        meetingId: Long,
        state: RecordingState,
        pauseReason: String? = null,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("UPDATE meetings SET endTime = :endTime, duration = :duration, status = :status, updatedAt = :updatedAt WHERE id = :meetingId")
    suspend fun finalizeMeeting(
        meetingId: Long,
        endTime: Long,
        duration: Long,
        status: MeetingStatus = MeetingStatus.STOPPED,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("UPDATE meetings SET transcriptStatus = :status, updatedAt = :updatedAt WHERE id = :meetingId")
    suspend fun updateTranscriptStatus(
        meetingId: Long,
        status: TranscriptStatus,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("UPDATE meetings SET summaryStatus = :status, updatedAt = :updatedAt WHERE id = :meetingId")
    suspend fun updateSummaryStatus(
        meetingId: Long,
        status: SummaryStatus,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("UPDATE meetings SET totalChunks = :totalChunks, updatedAt = :updatedAt WHERE id = :meetingId")
    suspend fun updateTotalChunks(
        meetingId: Long,
        totalChunks: Int,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("UPDATE meetings SET transcribedChunks = :transcribedChunks, updatedAt = :updatedAt WHERE id = :meetingId")
    suspend fun updateTranscribedChunks(
        meetingId: Long,
        transcribedChunks: Int,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("SELECT * FROM meetings WHERE status IN ('RECORDING', 'PAUSED') LIMIT 1")
    suspend fun getActiveMeeting(): Meeting?
}
