package org.logistix.domain.action;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Result returned by an ActionExecutor after invoking an authorized action against an external enterprise system.
 */
public record ActionResult(
        String actionId,
        ActionStatus status,
        String operationId,
        String message,
        Map<String, Object> outputData,
        Instant executedAt,
        Duration executionLatency,
        String errorDetails
) {
    public ActionResult {
        Objects.requireNonNull(actionId, "actionId must not be null");
        Objects.requireNonNull(status, "status must not be null");
        operationId = operationId != null ? operationId : "OP-" + actionId;
        message = message != null ? message : "";
        outputData = outputData != null ? Collections.unmodifiableMap(new LinkedHashMap<>(outputData)) : Collections.emptyMap();
        executedAt = executedAt != null ? executedAt : Instant.now();
        executionLatency = executionLatency != null ? executionLatency : Duration.ZERO;
        errorDetails = errorDetails != null ? errorDetails : "";
    }

    public static ActionResult success(String actionId, String operationId, String message, Map<String, Object> outputData, Duration latency) {
        return new ActionResult(
                actionId,
                ActionStatus.EXECUTED,
                operationId,
                message,
                outputData,
                Instant.now(),
                latency,
                ""
        );
    }

    public static ActionResult failure(String actionId, String operationId, String message, String errorDetails, Duration latency) {
        return new ActionResult(
                actionId,
                ActionStatus.FAILED,
                operationId,
                message,
                Collections.emptyMap(),
                Instant.now(),
                latency,
                errorDetails
        );
    }

    public boolean isSuccess() {
        return status == ActionStatus.EXECUTED;
    }
}
