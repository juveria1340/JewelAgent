package com.s2886810.jewelagent.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

/**
 * AgentService runs the full agentic loop.
 *
 * How it works step by step:
 *
 *  1. We send Claude an initial prompt + the list of tools it can call
 *  2. Claude either:
 *     a) Calls a tool  → we execute it, send the result back, go to step 2
 *     b) Writes a final answer → we return it
 *
 * The agent decides on its own which tools to call and in what order.
 * We just execute whatever it asks for and keep feeding the results back.
 * This loop continues until Claude stops calling tools and writes its report.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentService {

    private final AgentToolService toolService;
    private final ObjectMapper     objectMapper;

    @Value("${anthropic.api.key:}")
    private String apiKey;

    private static final String API_URL   = "https://api.anthropic.com/v1/messages";
    private static final String MODEL     = "claude-haiku-4-5-20251001";
    private static final int    MAX_TURNS = 10;  // safety limit — prevents infinite loops

    public AgentResponse runAdvisor() {
        if (apiKey == null || apiKey.isBlank()) {
            return AgentResponse.disabled();
        }

        log.info("Agent: starting autonomous advisor loop");

        // Conversation history grows as the agent calls tools and receives results
        List<ObjectNode> messages = new ArrayList<>();

        // Step 1 — send the initial prompt
        ObjectNode userMessage = objectMapper.createObjectNode();
        userMessage.put("role", "user");
        userMessage.put("content",
                """
                You are an autonomous inventory advisor for a jewellery shop.
                Using the tools available to you, analyse the current sales data
                and produce a clear, actionable advisory report covering:
    
                1. Today's sales performance summary
                2. Best performing categories this week
                3. Categories that need attention or restocking
                4. Specific recommendations the shop owner should act on today
    
                Use the tools to gather what you need, then write your final report.
                Write in plain text only. Do not use markdown, symbols, bullet points,
                asterisks, hashtags, or table formatting. Use plain numbered lists
                and simple paragraphs only.
                """
        );
        messages.add(userMessage);

        StringBuilder toolCallLog = new StringBuilder();
        String        finalReport = null;
        HttpClient    client      = HttpClient.newHttpClient();

        // Step 2 — agentic loop
        for (int turn = 0; turn < MAX_TURNS; turn++) {
            log.info("Agent: turn {}", turn + 1);

            try {
                // Build the API request body
                ObjectNode requestBody = objectMapper.createObjectNode();
                requestBody.put("model", MODEL);
                requestBody.put("max_tokens", 1024);
                requestBody.set("tools", toolService.buildToolDefinitions());

                ArrayNode messagesArray = requestBody.putArray("messages");
                messages.forEach(messagesArray::add);

                // Call the Anthropic API
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(API_URL))
                        .header("Content-Type", "application/json")
                        .header("x-api-key", apiKey)
                        .header("anthropic-version", "2023-06-01")
                        .POST(HttpRequest.BodyPublishers.ofString(
                                objectMapper.writeValueAsString(requestBody)))
                        .build();

                HttpResponse<String> response = client.send(
                        request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() != 200) {
                    log.error("Anthropic API error: HTTP {}", response.statusCode());
                    return AgentResponse.error("API error: HTTP " + response.statusCode());
                }

                JsonNode responseJson = objectMapper.readTree(response.body());
                String   stopReason  = responseJson.path("stop_reason").asText();
                JsonNode content     = responseJson.path("content");

                // Add Claude's response to the conversation history
                ObjectNode assistantMessage = objectMapper.createObjectNode();
                assistantMessage.put("role", "assistant");
                assistantMessage.set("content", content);
                messages.add(assistantMessage);

                // ── Case A: Claude finished — extract the final report ────────
                if ("end_turn".equals(stopReason)) {
                    for (JsonNode block : content) {
                        if ("text".equals(block.path("type").asText())) {
                            finalReport = block.path("text").asText();
                            break;
                        }
                    }
                    log.info("Agent: completed in {} turn(s)", turn + 1);
                    break;
                }

                // ── Case B: Claude wants to call tools ───────────────────────
                if ("tool_use".equals(stopReason)) {
                    ArrayNode toolResults = objectMapper.createArrayNode();

                    for (JsonNode block : content) {
                        if (!"tool_use".equals(block.path("type").asText())) continue;

                        String   toolName  = block.path("name").asText();
                        String   toolUseId = block.path("id").asText();
                        JsonNode input     = block.path("input");

                        log.info("Agent: calling tool '{}'", toolName);
                        toolCallLog.append("-> Tool called: ").append(toolName).append("\n");

                        // Execute the tool and get the result
                        String result = toolService.executeTool(toolName, input);
                        log.info("Agent: tool '{}' returned: {}", toolName, result);
                        toolCallLog.append("   Result: ").append(result).append("\n\n");

                        // Package the result to send back to Claude
                        ObjectNode toolResult = objectMapper.createObjectNode();
                        toolResult.put("type", "tool_result");
                        toolResult.put("tool_use_id", toolUseId);
                        toolResult.put("content", result);
                        toolResults.add(toolResult);
                    }

                    // Feed all tool results back into the conversation
                    ObjectNode toolResultMessage = objectMapper.createObjectNode();
                    toolResultMessage.put("role", "user");
                    toolResultMessage.set("content", toolResults);
                    messages.add(toolResultMessage);
                }

            } catch (Exception e) {
                log.error("Agent error on turn {}: {}", turn + 1, e.getMessage(), e);
                return AgentResponse.error("Agent error: " + e.getMessage());
            }
        }

        if (finalReport == null) {
            return AgentResponse.error(
                    "Agent did not produce a final report within " + MAX_TURNS + " turns.");
        }

        return AgentResponse.success(finalReport, toolCallLog.toString().trim());
    }
}