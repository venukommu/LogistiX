package org.logistix.ai.dispatch;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Strongly typed telemetry record tracking AI execution metadata, latency, invocation count,
 * provider details, provider type (LIVE vs MOCK), and fallback status.
 */
public record AITelemetry(
        String providerName,
        String providerType,
        String modelName,
        String promptVersion,
        int invocationCount,
        Duration latency,
        String status,
        Double advisoryConfidence,
        RiskLevel riskLevel,
        boolean fallbackTriggered,
        String failureReason,
        String correlationId,
        Instant timestamp
) {
    public AITelemetry {
        providerName = providerName != null ? providerName : "UNKNOWN";
        providerType = providerType != null ? providerType : "MOCK";
        modelName = modelName != null ? modelName : "UNKNOWN";
        promptVersion = promptVersion != null ? promptVersion : "DRIVER_DISPATCH_AI_PROMPT_V1";
        latency = latency != null ? latency : Duration.ZERO;
        status = status != null ? status : "NOT_EXECUTED";
        correlationId = correlationId != null ? correlationId : "";
        timestamp = timestamp != null ? timestamp : Instant.now();
    }

    public static AITelemetry success(
            String providerName,
            String providerType,
            String modelName,
            String promptVersion,
            int invocationCount,
            Duration latency,
            Double advisoryConfidence,
            RiskLevel riskLevel,
            String correlationId
    ) {
        return new AITelemetry(
                providerName,
                providerType,
                modelName,
                promptVersion,
                invocationCount,
                latency,
                "SUCCESS",
                advisoryConfidence,
                riskLevel,
                false,
                null,
                correlationId,
                Instant.now()
        );
    }

    public static AITelemetry fallback(
            String providerName,
            String providerType,
            String modelName,
            String promptVersion,
            Duration latency,
            String failureReason,
            String correlationId
    ) {
        return new AITelemetry(
                providerName,
                providerType,
                modelName,
                promptVersion,
                1,
                latency,
                "FALLBACK_TRIGGERED",
                null,
                null,
                true,
                failureReason,
                correlationId,
                Instant.now()
        );
    }

    public static AITelemetry skipped(String reason) {
        return new AITelemetry(
                "NONE",
                "NONE",
                "NONE",
                "DRIVER_DISPATCH_AI_PROMPT_V1",
                0,
                Duration.ZERO,
                "SKIPPED",
                null,
                null,
                false,
                reason,
                "",
                Instant.now()
        );
    }
}
