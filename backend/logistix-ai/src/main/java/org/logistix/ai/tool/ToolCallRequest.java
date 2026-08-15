package org.logistix.ai.tool;

/**
 * Standard request initiated by an LLM to call a registered tool.
 */
public record ToolCallRequest(
        String callId,
        String toolName,
        String jsonArguments
) {}
