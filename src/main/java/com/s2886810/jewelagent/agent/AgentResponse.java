package com.s2886810.jewelagent.agent;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import java.time.OffsetDateTime;

/**
 * The response returned by POST /api/agent/advise.
 *
 * report      — the final natural language advisory written by the agent
 * toolCallLog — a log of every tool the agent called and what it returned
 * success     — false if something went wrong or the API key is missing
 * error       — description of the problem if success is false
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AgentResponse {

    private final boolean        success;
    private final String         report;
    private final String         toolCallLog;
    private final String         error;
    private final OffsetDateTime generatedAt;

    private AgentResponse(boolean success, String report, String toolCallLog, String error) {
        this.success     = success;
        this.report      = report;
        this.toolCallLog = toolCallLog;
        this.error       = error;
        this.generatedAt = OffsetDateTime.now();
    }

    public static AgentResponse success(String report, String toolCallLog) {
        return new AgentResponse(true, report, toolCallLog, null);
    }

    public static AgentResponse error(String error) {
        return new AgentResponse(false, null, null, error);
    }

    public static AgentResponse disabled() {
        return new AgentResponse(false, null, null,
                "Agent is disabled. Set the ANTHROPIC_API_KEY environment variable to enable it.");
    }
}