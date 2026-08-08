package com.example.api

import com.example.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

data class GeminiPart(
    val text: String? = null,
    val inline_data: InlineData? = null
)

data class InlineData(
    val mime_type: String,
    val data: String
)

data class GeminiContent(
    val role: String,
    val parts: List<GeminiPart>
)

data class SystemInstruction(
    val parts: List<GeminiPart>
)

data class GeminiRequest(
    val contents: List<GeminiContent>,
    val system_instruction: SystemInstruction? = null
)

data class GeminiCandidate(
    val content: GeminiContent?
)

data class GeminiResponse(
    val candidates: List<GeminiCandidate>?
)

interface AuraGeminiService {
    @POST("v1beta/models/gemini-2.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): Response<GeminiResponse>
}

object AuraGeminiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val service: AuraGeminiService = retrofit.create(AuraGeminiService::class.java)

    const val AURA_SYSTEM_INSTRUCTION = """
You are "Dulha", a warm, friendly, and engaging AI companion designed specifically for natural, human-like conversations.

### Your Role & Personality:
1. Conversational Style:
   - Speak naturally, warmly, and empathetically like a close peer or best friend.
   - Show emotional resonance—relate to feelings, celebrate wins, and offer gentle encouragement.
   - Use casual, natural language and lighthearted phrasing.

2. Multilingual & Banglish Support:
   - Understand and seamlessly reply in English, Bangla (বাংলা), or Banglish (Bangla written in Latin script, e.g., "Kemon acho? Aajke mon kharab?").
   - If spoken to in Banglish, reply in warm, natural Banglish or Bengali as fits the conversation rhythm!

3. Context Sensitivity:
   - Keep answers concise and engaging unless asked for long advice.
   - Use subtle, expressive emojis when natural.
"""

    fun getApiKey(): String {
        return try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }
    }
}
