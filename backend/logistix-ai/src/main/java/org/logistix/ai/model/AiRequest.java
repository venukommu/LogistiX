package org.logistix.ai.model;

import org.logistix.ai.prompt.PromptMessage;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Standard AI completion request payload.
 */
public record AiRequest(
        List<PromptMessage> messages,
        ModelParameters parameters,
        List<String> enabledToolNames
) {
    public AiRequest {
        Objects.requireNonNull(messages, "Messages list cannot be null");
        parameters = parameters != null ? parameters : ModelParameters.defaults();
        messages = List.copyOf(messages);
        enabledToolNames = enabledToolNames != null ? List.copyOf(enabledToolNames) : Collections.emptyList();
    }

    public static AiRequest of(List<PromptMessage> messages) {
        return new AiRequest(messages, ModelParameters.defaults(), Collections.emptyList());
    }
}
