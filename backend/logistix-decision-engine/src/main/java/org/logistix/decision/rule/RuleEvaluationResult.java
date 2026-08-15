package org.logistix.decision.rule;

/**
 * Result outcome of executing an individual business or operational rule.
 */
public record RuleEvaluationResult(
        String ruleId,
        String ruleName,
        boolean passed,
        String reason,
        double penaltyOrBonusWeight
) {
    public static RuleEvaluationResult passed(String ruleId, String ruleName) {
        return new RuleEvaluationResult(ruleId, ruleName, true, "Rule criteria satisfied", 0.0);
    }

    public static RuleEvaluationResult failed(String ruleId, String ruleName, String reason, double penaltyWeight) {
        return new RuleEvaluationResult(ruleId, ruleName, false, reason, penaltyWeight);
    }
}
