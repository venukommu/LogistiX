package org.logistix.decision.explainability;

import java.util.Objects;

/**
 * Encapsulates a recommended domain choice alongside its mathematical score and explainability breakdown.
 *
 * @param <T> The recommended entity or plan type
 */
public record ExplainableRecommendation<T>(
        T recommendation,
        double score,
        int rank,
        DecisionExplanation explanation
) {
    public ExplainableRecommendation {
        Objects.requireNonNull(recommendation, "Recommendation cannot be null");
        Objects.requireNonNull(explanation, "Explanation cannot be null");
    }
}
