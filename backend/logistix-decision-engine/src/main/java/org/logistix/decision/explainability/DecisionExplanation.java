package org.logistix.decision.explainability;

import org.logistix.decision.rule.RuleEvaluationResult;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Transparent, human-interpretable justification for an AI or algorithmic decision.
 */
public record DecisionExplanation(
        String summary,
        double confidenceScore,
        List<FeatureImportance> featureImportances,
        List<String> keyFactors,
        List<RuleEvaluationResult> ruleOutcomes,
        List<String> tradeOffsConsidered
) {
    public DecisionExplanation {
        Objects.requireNonNull(summary, "Summary cannot be null");
        featureImportances = featureImportances != null ? List.copyOf(featureImportances) : Collections.emptyList();
        keyFactors = keyFactors != null ? List.copyOf(keyFactors) : Collections.emptyList();
        ruleOutcomes = ruleOutcomes != null ? List.copyOf(ruleOutcomes) : Collections.emptyList();
        tradeOffsConsidered = tradeOffsConsidered != null ? List.copyOf(tradeOffsConsidered) : Collections.emptyList();
    }
}
