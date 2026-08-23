package org.logistix.ai.dispatch;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Explicit, structured DTO representing candidate driver context for LLM prompt generation.
 * Eliminates string-based object dumping (e.g. toString()).
 */
public record CandidatePromptContext(
        String candidateId,
        String driverName,
        double deadheadDistanceKm,
        long deadheadDurationMinutes,
        long linehaulDurationMinutes,
        String scheduledDeliveryTime,
        double driverRating,
        double historicalOnTimeRate,
        String driverTier,
        double deterministicScore,
        List<String> activeRuleSignals
) {
    public CandidatePromptContext {
        Objects.requireNonNull(candidateId, "Candidate ID must not be null");
        Objects.requireNonNull(driverName, "Driver name must not be null");
        scheduledDeliveryTime = scheduledDeliveryTime != null ? scheduledDeliveryTime : "UNKNOWN";
        driverTier = driverTier != null ? driverTier : "STANDARD";
        activeRuleSignals = activeRuleSignals != null ? List.copyOf(activeRuleSignals) : Collections.emptyList();
    }
}
