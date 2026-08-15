package org.logistix.domain.rule;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Outcome and telemetry of evaluating a business or operational rule.
 */
public record RuleOutcome(
        String ruleId,
        String ruleName,
        boolean passed,
        String reason,
        double scoreAdjustment,
        Map<String, Object> metadata
) {
    public RuleOutcome {
        Objects.requireNonNull(ruleId, "Rule ID must not be null");
        Objects.requireNonNull(ruleName, "Rule name must not be null");
        Objects.requireNonNull(reason, "Reason must not be null");
        metadata = metadata != null ? Map.copyOf(metadata) : Collections.emptyMap();
    }

    public static RuleOutcome passed(String ruleId, String ruleName, String reason) {
        return new RuleOutcome(ruleId, ruleName, true, reason, 0.0, Collections.emptyMap());
    }

    public static RuleOutcome passed(String ruleId, String ruleName, String reason, double scoreBonus) {
        return new RuleOutcome(ruleId, ruleName, true, reason, scoreBonus, Collections.emptyMap());
    }

    public static RuleOutcome failed(String ruleId, String ruleName, String reason, double scorePenalty) {
        return new RuleOutcome(ruleId, ruleName, false, reason, scorePenalty, Collections.emptyMap());
    }
}
