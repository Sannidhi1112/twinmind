package com.voicerecorder.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.voicerecorder.data.local.dao.AudioChunkDao
import com.voicerecorder.data.local.dao.MeetingDao
import com.voicerecorder.data.local.dao.SummaryDao
import com.voicerecorder.data.local.entity.AudioChunk
import com.voicerecorder.data.local.entity.Meeting
import com.voicerecorder.data.local.entity.Summary

@Database(entities = [Meeting::class, AudioChunk::class, Summary::class], version = 2)
abstract class AppDatabase : RoomDatabase() {
    abstract fun meetingDao(): MeetingDao
    abstract fun audioChunkDao(): AudioChunkDao
    abstract fun summaryDao(): SummaryDao
}
