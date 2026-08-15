package org.logistix.dsl.cli;

import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Execution environment context passed to CLI command handlers.
 */
public record CliExecutionContext(
        Path workingDirectory,
        List<String> positionalArguments,
        Map<String, String> options,
        PrintWriter outputWriter,
        PrintWriter errorWriter
) {
    public CliExecutionContext {
        Objects.requireNonNull(workingDirectory, "Working directory must not be null");
        positionalArguments = positionalArguments != null ? List.copyOf(positionalArguments) : Collections.emptyList();
        options = options != null ? Map.copyOf(options) : Collections.emptyMap();
        outputWriter = outputWriter != null ? outputWriter : new PrintWriter(System.out, true);
        errorWriter = errorWriter != null ? errorWriter : new PrintWriter(System.err, true);
    }
}
