package com.voicerecorder.data.local.dao

import androidx.room.*
import com.voicerecorder.data.local.entity.AudioChunk
import com.voicerecorder.data.local.entity.ChunkStatus
import com.voicerecorder.data.local.entity.TranscriptStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface AudioChunkDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(audioChunk: AudioChunk): Long

    @Update
    suspend fun update(audioChunk: AudioChunk)

    @Delete
    suspend fun delete(audioChunk: AudioChunk)

    @Query("SELECT * FROM audio_chunks WHERE id = :chunkId")
    suspend fun getChunkById(chunkId: Long): AudioChunk?

    @Query("SELECT * FROM audio_chunks WHERE meetingId = :meetingId ORDER BY chunkNumber ASC")
    suspend fun getChunksByMeetingId(meetingId: Long): List<AudioChunk>

    @Query("SELECT * FROM audio_chunks WHERE meetingId = :meetingId ORDER BY chunkNumber ASC")
    fun getChunksByMeetingIdFlow(meetingId: Long): Flow<List<AudioChunk>>

    @Query("SELECT * FROM audio_chunks WHERE meetingId = :meetingId AND transcriptStatus = :status ORDER BY chunkNumber ASC")
    suspend fun getChunksByTranscriptStatus(meetingId: Long, status: TranscriptStatus): List<AudioChunk>

    @Query("UPDATE audio_chunks SET status = :status, updatedAt = :updatedAt WHERE id = :chunkId")
    suspend fun updateChunkStatus(
        chunkId: Long,
        status: ChunkStatus,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("UPDATE audio_chunks SET transcriptStatus = :status, transcriptText = :transcriptText, updatedAt = :updatedAt WHERE id = :chunkId")
    suspend fun updateTranscript(
        chunkId: Long,
        status: TranscriptStatus,
        transcriptText: String?,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("UPDATE audio_chunks SET transcriptStatus = :status, errorMessage = :errorMessage, retryCount = retryCount + 1, updatedAt = :updatedAt WHERE id = :chunkId")
    suspend fun updateTranscriptError(
        chunkId: Long,
        status: TranscriptStatus,
        errorMessage: String,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("SELECT COUNT(*) FROM audio_chunks WHERE meetingId = :meetingId")
    suspend fun getChunkCount(meetingId: Long): Int

    @Query("SELECT COUNT(*) FROM audio_chunks WHERE meetingId = :meetingId AND transcriptStatus = 'COMPLETED'")
    suspend fun getTranscribedChunkCount(meetingId: Long): Int

    @Query("SELECT * FROM audio_chunks WHERE meetingId = :meetingId ORDER BY chunkNumber DESC LIMIT 1")
    suspend fun getLatestChunk(meetingId: Long): AudioChunk?

    @Query("DELETE FROM audio_chunks WHERE meetingId = :meetingId")
    suspend fun deleteChunksByMeetingId(meetingId: Long)
}
