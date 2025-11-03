package com.voicerecorder.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "audio_chunks",
    foreignKeys = [
        ForeignKey(
            entity = Meeting::class,
            parentColumns = ["id"],
            childColumns = ["meetingId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("meetingId"), Index("chunkNumber")]
)
data class AudioChunk(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val meetingId: Long,
    val chunkNumber: Int,
    val filePath: String,
    val duration: Long, // in milliseconds
    val fileSize: Long, // in bytes
    val status: ChunkStatus = ChunkStatus.RECORDED,
    val transcriptText: String? = null,
    val transcriptStatus: TranscriptStatus = TranscriptStatus.PENDING,
    val retryCount: Int = 0,
    val errorMessage: String? = null,
    val startTime: Long,
    val endTime: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class ChunkStatus {
    RECORDING,
    RECORDED,
    UPLOADING,
    UPLOADED,
    TRANSCRIBING,
    TRANSCRIBED,
    FAILED,
    DELETED
}
