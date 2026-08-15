package org.logistix.ai.prompt;

import java.util.Objects;

/**
 * Immutable single prompt message.
 */
public record PromptMessage(
        PromptRole role,
        String content
) {
    public PromptMessage {
        Objects.requireNonNull(role, "Prompt role cannot be null");
        Objects.requireNonNull(content, "Prompt content cannot be null");
    }

    public static PromptMessage system(String content) {
        return new PromptMessage(PromptRole.SYSTEM, content);
    }

    public static PromptMessage user(String content) {
        return new PromptMessage(PromptRole.USER, content);
    }

    public static PromptMessage assistant(String content) {
        return new PromptMessage(PromptRole.ASSISTANT, content);
    }

    public static PromptMessage tool(String content) {
        return new PromptMessage(PromptRole.TOOL, content);
    }
}
