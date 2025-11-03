package com.voicerecorder.di

import android.content.Context
import androidx.room.Room
import com.voicerecorder.data.local.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "voice_recorder_db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideMeetingDao(appDatabase: AppDatabase) = appDatabase.meetingDao()

    @Provides
    fun provideAudioChunkDao(appDatabase: AppDatabase) = appDatabase.audioChunkDao()

    @Provides
    fun provideSummaryDao(appDatabase: AppDatabase) = appDatabase.summaryDao()
}
