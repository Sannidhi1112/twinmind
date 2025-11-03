package com.voicerecorder.di

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.voicerecorder.data.remote.api.MockApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideGson(): Gson {
        return GsonBuilder()
            .setLenient()
            .create()
    }

    @Provides
    @Singleton
    fun provideMockApiService(): MockApiService {
        return MockApiService()
    }
}
