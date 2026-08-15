package org.logistix.dsl.cli;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Result returned by a CLI command execution.
 */
public record CliExecutionResult(
        int exitCode,
        String message,
        Map<String, Object> outputAttributes
) {
    public CliExecutionResult {
        Objects.requireNonNull(message, "Message must not be null");
        outputAttributes = outputAttributes != null ? Map.copyOf(outputAttributes) : Collections.emptyMap();
    }

    public static CliExecutionResult success(String message) {
        return new CliExecutionResult(0, message, Collections.emptyMap());
    }

    public static CliExecutionResult failure(int exitCode, String errorMessage) {
        return new CliExecutionResult(exitCode, errorMessage, Collections.emptyMap());
    }
}
