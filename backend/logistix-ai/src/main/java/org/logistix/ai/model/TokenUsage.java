package org.logistix.ai.model;

/**
 * Token usage telemetry reported by the AI model provider.
 */
public record TokenUsage(
        long promptTokens,
        long generationTokens,
        long totalTokens
) {
    public static TokenUsage empty() {
        return new TokenUsage(0, 0, 0);
    }
}
