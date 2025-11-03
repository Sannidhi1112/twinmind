package com.twinmind.voicerecorder.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "meetings")
data class Meeting(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String = "Untitled Meeting",
    val startTime: Long,
    val endTime: Long? = null,
    val duration: Long = 0, // in milliseconds
    val status: MeetingStatus = MeetingStatus.RECORDING,
    val audioFilePath: String? = null,
    val transcriptStatus: TranscriptStatus = TranscriptStatus.PENDING,
    val summaryStatus: SummaryStatus = SummaryStatus.PENDING,
    val recordingState: RecordingState = RecordingState.IDLE,
    val pauseReason: String? = null,
    val totalChunks: Int = 0,
    val transcribedChunks: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class MeetingStatus {
    RECORDING,
    PAUSED,
    STOPPED,
    COMPLETED
}

enum class TranscriptStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    FAILED
}

enum class SummaryStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    FAILED
}

enum class RecordingState {
    IDLE,
    RECORDING,
    PAUSED_PHONE_CALL,
    PAUSED_AUDIO_FOCUS_LOSS,
    PAUSED_NO_AUDIO,
    STOPPED,
    ERROR
}
