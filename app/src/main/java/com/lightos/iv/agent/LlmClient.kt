package com.lightos.iv.agent

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * OpenAI-compatible chat-completions client. Point `baseUrl` at OpenAI, OpenRouter,
 * Groq, Together, etc. — or at a local Ollama / llama.cpp / vLLM server
 * (e.g. "http://localhost:11434/v1") for fully on-device inference.
 */
class LlmClient(
    private val baseUrl: String,
    private val apiKey: String,
) {
    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = false }

    suspend fun chat(model: String, messages: List<ChatMessage>, tools: List<ToolDef>): ChatResponse =
        withContext(Dispatchers.IO) {
            val chatRequest = ChatRequest(
                model = model,
                messages = messages,
                tools = if (tools.isEmpty()) null else tools,
                temperature = 0.2,
            )
            val body = json.encodeToString(ChatRequest.serializer(), chatRequest)
            val httpRequest = Request.Builder()
                .url("${baseUrl.trimEnd('/')}/chat/completions")
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(body.toRequestBody(JSON))
                .build()
            http.newCall(httpRequest).execute().use { resp ->
                val raw = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    throw LlmException("HTTP ${resp.code}: ${raw.take(400)}")
                }
                json.decodeFromString(ChatResponse.serializer(), raw)
            }
        }

    companion object {
        private val JSON = "application/json; charset=utf-8".toMediaType()
    }
}

class LlmException(message: String) : RuntimeException(message)

@Serializable
data class ChatMessage(
    val role: String,
    val content: String? = null,
    @SerialName("tool_calls") val toolCalls: List<ToolCall>? = null,
    @SerialName("tool_call_id") val toolCallId: String? = null,
    val name: String? = null,
)

@Serializable
data class ToolCall(
    val id: String,
    val type: String = "function",
    val function: ToolCallFunction,
)

@Serializable
data class ToolCallFunction(
    val name: String,
    /** Raw JSON string of arguments from the model. Parsed by the executor. */
    val arguments: String,
)

@Serializable
data class ToolDef(
    val type: String = "function",
    val function: ToolDefFunction,
)

@Serializable
data class ToolDefFunction(
    val name: String,
    val description: String,
    val parameters: JsonElement,
)

@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val tools: List<ToolDef>? = null,
    @SerialName("tool_choice") val toolChoice: String = "auto",
    // No default: with encodeDefaults=false a default-valued prop is dropped, and we want
    // a low temperature for a tool-calling agent.
    val temperature: Double,
)

@Serializable
data class ChatResponse(
    val choices: List<Choice> = emptyList(),
)

@Serializable
data class Choice(
    val message: ChatMessage,
    @SerialName("finish_reason") val finishReason: String? = null,
)
