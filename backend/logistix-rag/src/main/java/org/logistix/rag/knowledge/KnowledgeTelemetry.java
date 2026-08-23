package org.logistix.rag.knowledge;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Strongly typed telemetry record tracking knowledge retrieval operations, latency,
 * evidence count, retrieved document IDs, provider details, and status.
 */
public record KnowledgeTelemetry(
        String providerName,
        String queryText,
        int requestedMaxResults,
        int retrievedCount,
        List<String> evidenceDocumentIds,
        Duration retrievalLatency,
        String status,
        boolean fallbackTriggered,
        String failureReason,
        String correlationId,
        Instant timestamp
) {
    public KnowledgeTelemetry {
        providerName = providerName != null ? providerName : "UNKNOWN";
        queryText = queryText != null ? queryText : "";
        evidenceDocumentIds = evidenceDocumentIds != null ? List.copyOf(evidenceDocumentIds) : Collections.emptyList();
        retrievalLatency = retrievalLatency != null ? retrievalLatency : Duration.ZERO;
        status = status != null ? status : "NOT_EXECUTED";
        correlationId = correlationId != null ? correlationId : "";
        timestamp = timestamp != null ? timestamp : Instant.now();
    }

    public static KnowledgeTelemetry success(
            String providerName,
            String queryText,
            int requestedMaxResults,
            List<String> documentIds,
            Duration latency,
            String correlationId
    ) {
        return new KnowledgeTelemetry(
                providerName,
                queryText,
                requestedMaxResults,
                documentIds != null ? documentIds.size() : 0,
                documentIds != null ? documentIds : Collections.emptyList(),
                latency,
                documentIds != null && !documentIds.isEmpty() ? "SUCCESS" : "EMPTY",
                false,
                null,
                correlationId,
                Instant.now()
        );
    }

    public static KnowledgeTelemetry fallback(
            String providerName,
            String queryText,
            int requestedMaxResults,
            Duration latency,
            String failureReason,
            String correlationId
    ) {
        return new KnowledgeTelemetry(
                providerName,
                queryText,
                requestedMaxResults,
                0,
                Collections.emptyList(),
                latency,
                "FALLBACK_TRIGGERED",
                true,
                failureReason,
                correlationId,
                Instant.now()
        );
    }

    public static KnowledgeTelemetry skipped(String reason) {
        return new KnowledgeTelemetry(
                "NONE",
                "",
                0,
                0,
                Collections.emptyList(),
                Duration.ZERO,
                "SKIPPED",
                false,
                reason,
                "",
                Instant.now()
        );
    }
}
