package com.twinmind.voicerecorder.data.remote.api

import retrofit2.Response
import retrofit2.http.*

interface SummaryApi {

    // OpenAI GPT API for summary generation
    @POST("v1/chat/completions")
    suspend fun generateSummaryOpenAI(
        @Body request: OpenAISummaryRequest
    ): Response<OpenAISummaryResponse>

    // Google Gemini API for summary generation
    @POST("v1/models/gemini-2.5-flash:streamGenerateContent")
    suspend fun generateSummaryGemini(
        @Body request: GeminiSummaryRequest
    ): Response<GeminiSummaryResponse>
}

// OpenAI Models
data class OpenAISummaryRequest(
    val model: String = "gpt-4",
    val messages: List<OpenAIMessage>,
    val temperature: Double = 0.7,
    val stream: Boolean = false
)

data class OpenAIMessage(
    val role: String,
    val content: String
)

data class OpenAISummaryResponse(
    val id: String,
    val choices: List<OpenAIChoice>
)

data class OpenAIChoice(
    val message: OpenAIMessage,
    val finishReason: String?
)

// Gemini Models
data class GeminiSummaryRequest(
    val contents: List<GeminiContent>,
    val generationConfig: GeminiGenerationConfig? = GeminiGenerationConfig(
        temperature = 0.7,
        maxOutputTokens = 8192
    )
)

data class GeminiSummaryResponse(
    val candidates: List<GeminiCandidate>
)
