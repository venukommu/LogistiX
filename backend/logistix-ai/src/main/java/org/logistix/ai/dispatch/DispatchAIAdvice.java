package org.logistix.ai.dispatch;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Strongly typed structured advice emitted by an AI model evaluating a feasible dispatch candidate.
 * The AI provides purely qualitative risk signals, context, and cited knowledge evidence; deterministic LogistiX policies govern all final scoring.
 */
public record DispatchAIAdvice(
        String candidateId,
        RiskLevel riskLevel,
        double advisoryConfidence,
        String reasoning,
        List<String> contributingFactors,
        List<String> warnings,
        List<String> knowledgeEvidenceUsed,
        Instant timestamp
) {
    public DispatchAIAdvice {
        Objects.requireNonNull(candidateId, "Candidate ID must not be null");
        riskLevel = riskLevel != null ? riskLevel : RiskLevel.LOW;
        reasoning = reasoning != null ? reasoning : "";
        contributingFactors = contributingFactors != null ? List.copyOf(contributingFactors) : Collections.emptyList();
        warnings = warnings != null ? List.copyOf(warnings) : Collections.emptyList();
        knowledgeEvidenceUsed = knowledgeEvidenceUsed != null ? List.copyOf(knowledgeEvidenceUsed) : Collections.emptyList();
        timestamp = timestamp != null ? timestamp : Instant.now();

        if (advisoryConfidence < 0.0 || advisoryConfidence > 1.0) {
            throw new IllegalArgumentException("Advisory confidence must be between 0.0 and 1.0");
        }
    }

    /**
     * Backward-compatible 7-argument constructor for existing callers.
     */
    public DispatchAIAdvice(
            String candidateId,
            RiskLevel riskLevel,
            double advisoryConfidence,
            String reasoning,
            List<String> contributingFactors,
            List<String> warnings,
            Instant timestamp
    ) {
        this(candidateId, riskLevel, advisoryConfidence, reasoning, contributingFactors, warnings, Collections.emptyList(), timestamp);
    }

    public static DispatchAIAdvice of(
            String candidateId,
            RiskLevel riskLevel,
            double advisoryConfidence,
            String reasoning,
            List<String> contributingFactors,
            List<String> warnings
    ) {
        return new DispatchAIAdvice(
                candidateId,
                riskLevel,
                advisoryConfidence,
                reasoning,
                contributingFactors,
                warnings,
                Collections.emptyList(),
                Instant.now()
        );
    }

    public static DispatchAIAdvice of(
            String candidateId,
            RiskLevel riskLevel,
            double advisoryConfidence,
            String reasoning,
            List<String> contributingFactors,
            List<String> warnings,
            List<String> knowledgeEvidenceUsed
    ) {
        return new DispatchAIAdvice(
                candidateId,
                riskLevel,
                advisoryConfidence,
                reasoning,
                contributingFactors,
                warnings,
                knowledgeEvidenceUsed,
                Instant.now()
        );
    }

    public static DispatchAIAdvice neutral(String candidateId, String reasoning) {
        return new DispatchAIAdvice(
                candidateId,
                RiskLevel.LOW,
                0.92,
                reasoning,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Instant.now()
        );
    }
}
