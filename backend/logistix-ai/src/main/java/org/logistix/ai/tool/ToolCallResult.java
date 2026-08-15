package org.logistix.ai.tool;

/**
 * Result of tool execution returned back to the model context.
 */
public record ToolCallResult(
        String callId,
        String toolName,
        boolean success,
        String jsonResult,
        String errorMessage
) {
    public static ToolCallResult success(String callId, String toolName, String jsonResult) {
        return new ToolCallResult(callId, toolName, true, jsonResult, null);
    }

    public static ToolCallResult failure(String callId, String toolName, String errorMessage) {
        return new ToolCallResult(callId, toolName, false, null, errorMessage);
    }
}
