package com.lightos.iv.agent

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

/** A single visible event in the agent's reasoning trace, shown in the agent sheet. */
data class AgentEvent(val kind: Kind, val text: String) {
    enum class Kind { Thought, Tool, Result, Answer, Error }
}

/**
 * The ReAct-style tool-calling loop. Each turn:
 *  1. Send the system prompt (always fresh) + conversation + tool definitions to the model.
 *  2. If it returns tool calls, execute them against real Android capabilities, feed the
 *     results back, and loop. Confirmation-gated tools ask the user before running.
 *  3. If it returns a plain message, that is the answer and the loop ends.
 *
 * Bounded by [MAX_ITERATIONS] so a misbehaving model can't loop forever.
 */
class Agent(
    private val client: LlmClient,
    private val tools: List<Tool>,
    private val systemPrompt: () -> String,
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun run(
        model: String,
        history: List<ChatMessage>,
        userText: String,
        host: ToolHost,
        onEvent: (AgentEvent) -> Unit,
    ): List<ChatMessage> {
        val toolDefs = tools.map { ToolDef(function = ToolDefFunction(it.name, it.description, it.parameters())) }
        var working = history + ChatMessage(role = "user", content = userText)
        val system = ChatMessage(role = "system", content = systemPrompt())

        repeat(MAX_ITERATIONS) {
            val response = try {
                client.chat(model, listOf(system) + working, toolDefs)
            } catch (e: Exception) {
                onEvent(AgentEvent(AgentEvent.Kind.Error, "Could not reach the model: ${e.message ?: e::class.simpleName}"))
                return working
            }
            val message = response.choices.firstOrNull()?.message
                ?: run {
                    onEvent(AgentEvent(AgentEvent.Kind.Error, "Empty response from model."))
                    return working
                }

            working = working + message

            val calls = message.toolCalls
            if (calls.isNullOrEmpty()) {
                onEvent(AgentEvent(AgentEvent.Kind.Answer, message.content.orEmpty()))
                return working
            }

            message.content?.takeIf { it.isNotBlank() }?.let { onEvent(AgentEvent(AgentEvent.Kind.Thought, it)) }

            for (call in calls) {
                onEvent(AgentEvent(AgentEvent.Kind.Tool, "${call.function.name}(${summarizeArgs(call)})"))
                val result = executeToolCall(call, host)
                onEvent(AgentEvent(AgentEvent.Kind.Result, result.content.take(400)))
                working = working + ChatMessage(
                    role = "tool",
                    content = result.content.take(MAX_RESULT_CHARS),
                    toolCallId = call.id,
                    name = call.function.name,
                )
            }
        }

        onEvent(AgentEvent(AgentEvent.Kind.Error, "Reached the action limit without finishing."))
        return working
    }

    private suspend fun executeToolCall(call: ToolCall, host: ToolHost): ToolResult {
        val tool = tools.firstOrNull { it.name == call.function.name }
            ?: return ToolResult.err("Unknown tool: ${call.function.name}")
        val args: JsonObject = try {
            json.decodeFromString(JsonObject.serializer(), call.function.arguments.ifBlank { "{}" })
        } catch (e: Exception) {
            JsonObject(emptyMap())
        }
        return try {
            if (tool.requiresConfirmation) {
                val approved = host.confirm("Confirm action", tool.summarize(args))
                if (!approved) ToolResult.ok("User declined to run this action.")
                else tool.run(args, host)
            } else {
                tool.run(args, host)
            }
        } catch (e: SecurityException) {
            ToolResult.err("Permission denied for ${tool.name}: ${e.message}")
        } catch (e: Exception) {
            ToolResult.err("${tool.name} failed: ${e.message ?: e::class.simpleName}")
        }
    }

    private fun summarizeArgs(call: ToolCall): String =
        runCatching { json.decodeFromString(JsonObject.serializer(), call.function.arguments) }
            .getOrNull()?.toString()?.take(80).orEmpty()

    companion object {
        private const val MAX_ITERATIONS = 6
        private const val MAX_RESULT_CHARS = 2000
    }
}
