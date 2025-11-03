package com.twinmind.voicerecorder.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.twinmind.voicerecorder.data.local.dao.AudioChunkDao
import com.twinmind.voicerecorder.data.local.dao.MeetingDao
import com.twinmind.voicerecorder.data.local.dao.SummaryDao
import com.twinmind.voicerecorder.data.local.entity.AudioChunk
import com.twinmind.voicerecorder.data.local.entity.Meeting
import com.twinmind.voicerecorder.data.local.entity.Summary

@Database(
    entities = [Meeting::class, AudioChunk::class, Summary::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class VoiceRecorderDatabase : RoomDatabase() {
    abstract fun meetingDao(): MeetingDao
    abstract fun audioChunkDao(): AudioChunkDao
    abstract fun summaryDao(): SummaryDao

    companion object {
        const val DATABASE_NAME = "voice_recorder_db"
    }
}
