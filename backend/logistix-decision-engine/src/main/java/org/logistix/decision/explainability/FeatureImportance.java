package org.logistix.decision.explainability;

/**
 * Quantifies the contribution and directional impact of a specific feature on a decision.
 */
public record FeatureImportance(
        String featureName,
        double weight,
        double contributionScore,
        ImpactDirection impactDirection,
        String description
) {
    public enum ImpactDirection {
        POSITIVE,
        NEGATIVE,
        NEUTRAL
    }
}
