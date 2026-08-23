package org.logistix.ai.dispatch;

import org.logistix.domain.ports.KnowledgeProvider.GroundingDocument;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Domain-neutral structured request payload passed across the AI boundary for single-call batched evaluation.
 * Integrates optional retrieved grounding enterprise knowledge evidence.
 */
public record DispatchAIRequest(
        String shipmentId,
        String originSummary,
        String destinationSummary,
        double weightKg,
        String deliveryDeadline,
        String weatherAdvisory,
        String executionMode,
        List<CandidatePromptContext> candidates,
        List<GroundingDocument> knowledgeEvidence
) {
    public DispatchAIRequest {
        Objects.requireNonNull(shipmentId, "Shipment ID must not be null");
        originSummary = originSummary != null ? originSummary : "UNKNOWN";
        destinationSummary = destinationSummary != null ? destinationSummary : "UNKNOWN";
        deliveryDeadline = deliveryDeadline != null ? deliveryDeadline : "UNKNOWN";
        weatherAdvisory = weatherAdvisory != null ? weatherAdvisory : "CLEAR";
        executionMode = executionMode != null ? executionMode : "HYBRID";
        candidates = candidates != null ? List.copyOf(candidates) : Collections.emptyList();
        knowledgeEvidence = knowledgeEvidence != null ? List.copyOf(knowledgeEvidence) : Collections.emptyList();
    }

    /**
     * Backward-compatible 8-argument constructor for existing callers.
     */
    public DispatchAIRequest(
            String shipmentId,
            String originSummary,
            String destinationSummary,
            double weightKg,
            String deliveryDeadline,
            String weatherAdvisory,
            String executionMode,
            List<CandidatePromptContext> candidates
    ) {
        this(shipmentId, originSummary, destinationSummary, weightKg, deliveryDeadline, weatherAdvisory, executionMode, candidates, Collections.emptyList());
    }
}
