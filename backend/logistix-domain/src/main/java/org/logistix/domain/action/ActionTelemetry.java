package org.logistix.domain.action;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Strongly typed telemetry record for enterprise action governance and execution.
 * Kept strictly segregated from AITelemetry and KnowledgeTelemetry.
 */
public record ActionTelemetry(
        String actionId,
        ActionType actionType,
        ActionStatus authorizationStatus,
        Duration governanceLatency,
        Duration executionLatency,
        String executorType,
        boolean executed,
        String correlationId,
        Instant timestamp
) {
    public ActionTelemetry {
        Objects.requireNonNull(actionId, "actionId must not be null");
        Objects.requireNonNull(actionType, "actionType must not be null");
        Objects.requireNonNull(authorizationStatus, "authorizationStatus must not be null");
        governanceLatency = governanceLatency != null ? governanceLatency : Duration.ZERO;
        executionLatency = executionLatency != null ? executionLatency : Duration.ZERO;
        executorType = executorType != null ? executorType : "NONE";
        correlationId = correlationId != null ? correlationId : actionId;
        timestamp = timestamp != null ? timestamp : Instant.now();
    }

    public static ActionTelemetry of(
            String actionId,
            ActionType actionType,
            ActionStatus authorizationStatus,
            Duration governanceLatency,
            Duration executionLatency,
            String executorType,
            boolean executed,
            String correlationId
    ) {
        return new ActionTelemetry(
                actionId,
                actionType,
                authorizationStatus,
                governanceLatency,
                executionLatency,
                executorType,
                executed,
                correlationId,
                Instant.now()
        );
    }
}
