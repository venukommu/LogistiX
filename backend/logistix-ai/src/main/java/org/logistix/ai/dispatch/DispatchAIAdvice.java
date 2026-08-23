package org.logistix.ai.dispatch;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Strongly typed structured advice emitted by an AI model evaluating a feasible dispatch candidate.
 */
public record DispatchAIAdvice(
        String candidateId,
        RiskLevel riskLevel,
        double advisoryConfidence,
        String reasoning,
        List<String> contributingFactors,
        List<String> warnings,
        double suggestedScoreAdjustment,
        Instant timestamp
) {
    public DispatchAIAdvice {
        Objects.requireNonNull(candidateId, "Candidate ID must not be null");
        riskLevel = riskLevel != null ? riskLevel : RiskLevel.LOW;
        reasoning = reasoning != null ? reasoning : "";
        contributingFactors = contributingFactors != null ? List.copyOf(contributingFactors) : Collections.emptyList();
        warnings = warnings != null ? List.copyOf(warnings) : Collections.emptyList();
        timestamp = timestamp != null ? timestamp : Instant.now();

        if (advisoryConfidence < 0.0 || advisoryConfidence > 1.0) {
            throw new IllegalArgumentException("Advisory confidence must be between 0.0 and 1.0");
        }
        if (suggestedScoreAdjustment < -0.50 || suggestedScoreAdjustment > 0.50) {
            throw new IllegalArgumentException("Suggested score adjustment must be bounded between -0.50 and +0.50");
        }
    }

    public static DispatchAIAdvice of(
            String candidateId,
            RiskLevel riskLevel,
            double advisoryConfidence,
            String reasoning,
            List<String> contributingFactors,
            List<String> warnings,
            double suggestedScoreAdjustment
    ) {
        return new DispatchAIAdvice(
                candidateId,
                riskLevel,
                advisoryConfidence,
                reasoning,
                contributingFactors,
                warnings,
                suggestedScoreAdjustment,
                Instant.now()
        );
    }

    public static DispatchAIAdvice neutral(String candidateId, String reasoning) {
        return new DispatchAIAdvice(
                candidateId,
                RiskLevel.LOW,
                0.85,
                reasoning,
                Collections.emptyList(),
                Collections.emptyList(),
                0.0,
                Instant.now()
        );
    }
}
