package org.logistix.model.plan;

import org.logistix.model.variable.DecisionVariables;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Runtime environment settings for an ExecutionPlan run.
 */
public record ExecutionContext(
        UUID runId,
        String environment,
        Duration timeout,
        DecisionVariables variables,
        Map<String, Object> parameters
) {
    public ExecutionContext {
        Objects.requireNonNull(runId, "Run ID must not be null");
        environment = environment != null ? environment : "default";
        timeout = timeout != null ? timeout : Duration.ofSeconds(10);
        variables = variables != null ? variables : DecisionVariables.empty();
        parameters = parameters != null ? Map.copyOf(parameters) : Collections.emptyMap();
    }

    public static ExecutionContext defaults() {
        return new ExecutionContext(UUID.randomUUID(), "default", Duration.ofSeconds(10), DecisionVariables.empty(), Collections.emptyMap());
    }
}
