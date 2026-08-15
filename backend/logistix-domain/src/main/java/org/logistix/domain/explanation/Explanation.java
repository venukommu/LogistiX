package org.logistix.domain.explanation;

import org.logistix.domain.rule.RuleOutcome;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Human-interpretable, auditable justification for an automated decision or recommendation.
 */
public record Explanation(
        String summary,
        double confidenceScore,
        List<FeatureContribution> featureContributions,
        List<RuleOutcome> ruleOutcomes,
        List<String> keyFactors,
        List<String> tradeOffsConsidered
) {
    public Explanation {
        Objects.requireNonNull(summary, "Explanation summary must not be null");
        featureContributions = featureContributions != null ? List.copyOf(featureContributions) : Collections.emptyList();
        ruleOutcomes = ruleOutcomes != null ? List.copyOf(ruleOutcomes) : Collections.emptyList();
        keyFactors = keyFactors != null ? List.copyOf(keyFactors) : Collections.emptyList();
        tradeOffsConsidered = tradeOffsConsidered != null ? List.copyOf(tradeOffsConsidered) : Collections.emptyList();
    }

    public static Explanation simple(String summary, double confidenceScore) {
        return new Explanation(summary, confidenceScore, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
    }
}
