package org.logistix.ai.model;

import java.time.Duration;
import java.util.Objects;

/**
 * Standard AI completion response payload.
 *
 * @param <T> Payload body type (String or structured DTO)
 */
public record AiResponse<T>(
        T content,
        String modelName,
        TokenUsage tokenUsage,
        Duration latency,
        String finishReason
) {
    public AiResponse {
        Objects.requireNonNull(content, "Content cannot be null");
        tokenUsage = tokenUsage != null ? tokenUsage : TokenUsage.empty();
    }
}
