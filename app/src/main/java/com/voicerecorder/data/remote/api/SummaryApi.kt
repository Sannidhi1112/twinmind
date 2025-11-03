package com.voicerecorder.data.remote.api

import com.google.gson.annotations.SerializedName
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Streaming

interface SummaryApi {

    @Streaming
    @POST("v1/chat/completions")
    suspend fun generateSummary(@Body body: SummaryRequest): ResponseBody
}

data class SummaryRequest(
    val model: String,
    val messages: List<Message>,
    val stream: Boolean = true
) {
    data class Message(
        val role: String,
        val content: String
    )
}

data class SummaryResponse(
    val choices: List<Choice>
) {
    data class Choice(
        val delta: Delta
    )

    data class Delta(
        val content: String?
    )
}
