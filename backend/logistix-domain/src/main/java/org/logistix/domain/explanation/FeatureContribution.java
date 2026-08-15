package org.logistix.domain.explanation;

/**
 * Quantifies the contribution and directional impact of a specific feature or fact.
 */
public record FeatureContribution(
        String featureName,
        double weight,
        double contributionScore,
        ImpactDirection impactDirection,
        String rationale
) {
    public enum ImpactDirection {
        POSITIVE,
        NEGATIVE,
        NEUTRAL
    }
}
