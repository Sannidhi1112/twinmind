package com.voicerecorder.data.local

import androidx.room.TypeConverter
import com.voicerecorder.data.local.entity.*

class Converters {

    @TypeConverter
    fun fromMeetingStatus(value: MeetingStatus): String {
        return value.name
    }

    @TypeConverter
    fun toMeetingStatus(value: String): MeetingStatus {
        return MeetingStatus.valueOf(value)
    }

    @TypeConverter
    fun fromTranscriptStatus(value: TranscriptStatus): String {
        return value.name
    }

    @TypeConverter
    fun toTranscriptStatus(value: String): TranscriptStatus {
        return TranscriptStatus.valueOf(value)
    }

    @TypeConverter
    fun fromSummaryStatus(value: SummaryStatus): String {
        return value.name
    }

    @TypeConverter
    fun toSummaryStatus(value: String): SummaryStatus {
        return SummaryStatus.valueOf(value)
    }

    @TypeConverter
    fun fromRecordingState(value: RecordingState): String {
        return value.name
    }

    @TypeConverter
    fun toRecordingState(value: String): RecordingState {
        return RecordingState.valueOf(value)
    }

    @TypeConverter
    fun fromChunkStatus(value: ChunkStatus): String {
        return value.name
    }

    @TypeConverter
    fun toChunkStatus(value: String): ChunkStatus {
        return ChunkStatus.valueOf(value)
    }
}
