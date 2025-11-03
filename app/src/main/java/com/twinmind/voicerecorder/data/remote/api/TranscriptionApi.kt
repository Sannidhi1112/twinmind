package com.twinmind.voicerecorder.data.remote.api

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface TranscriptionApi {

    // OpenAI Whisper API
    @Multipart
    @POST("v1/audio/transcriptions")
    suspend fun transcribeAudioOpenAI(
        @Part file: MultipartBody.Part,
        @Part("model") model: RequestBody,
        @Part("language") language: RequestBody? = null,
        @Part("prompt") prompt: RequestBody? = null
    ): Response<TranscriptionResponse>

    // Google Gemini API (if using Gemini for transcription)
    @POST("v1/models/gemini-2.5-flash:generateContent")
    suspend fun transcribeAudioGemini(
        @Body request: GeminiTranscriptionRequest
    ): Response<GeminiTranscriptionResponse>
}

data class TranscriptionResponse(
    val text: String
)

data class GeminiTranscriptionRequest(
    val contents: List<GeminiContent>,
    val generationConfig: GeminiGenerationConfig? = null
)

data class GeminiContent(
    val parts: List<GeminiPart>
)

data class GeminiPart(
    val text: String? = null,
    val inlineData: GeminiInlineData? = null
)

data class GeminiInlineData(
    val mimeType: String,
    val data: String // Base64 encoded audio
)

data class GeminiGenerationConfig(
    val temperature: Double? = 0.1,
    val maxOutputTokens: Int? = 8192
)

data class GeminiTranscriptionResponse(
    val candidates: List<GeminiCandidate>
)

data class GeminiCandidate(
    val content: GeminiContent,
    val finishReason: String? = null
)
