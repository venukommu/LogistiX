package org.logistix.examples.dispatch.rules;

import org.logistix.domain.decision.DecisionContext;
import org.logistix.domain.rule.Rule;
import org.logistix.domain.rule.RuleOutcome;
import org.logistix.examples.dispatch.model.DispatchCandidate;

import java.time.Duration;
import java.util.Map;

/**
 * Deterministic business rule applying a penalty if a driver is close to their mandatory rest break,
 * avoiding fatigue risk during the initial segment of transit.
 */
public class RestBalanceRule implements Rule<DispatchCandidate> {

    public static final String RULE_ID = "RULE_REST_BALANCE";
    public static final String RULE_NAME = "Driver Rest Balance & Fatigue Safeguard";

    private final Duration restThreshold;

    public RestBalanceRule() {
        this(Duration.ofMinutes(90));
    }

    public RestBalanceRule(Duration restThreshold) {
        this.restThreshold = restThreshold;
    }

    @Override
    public String getId() {
        return RULE_ID;
    }

    @Override
    public String getName() {
        return RULE_NAME;
    }

    @Override
    public int getPriority() {
        return 90;
    }

    @Override
    public RuleOutcome evaluate(DispatchCandidate candidate, DecisionContext context) {
        Duration timeUntilRest = candidate.driver().timeUntilMandatoryRest();
        if (timeUntilRest.compareTo(restThreshold) <= 0) {
            double penalty = -0.10;
            return new RuleOutcome(
                    RULE_ID,
                    RULE_NAME,
                    false,
                    String.format("Driver '%s' is within %d min of mandatory rest (penalty: %.2f)",
                            candidate.driver().name(), timeUntilRest.toMinutes(), penalty),
                    penalty,
                    Map.of("timeUntilRestMinutes", timeUntilRest.toMinutes(), "penalty", penalty)
            );
        }

        return RuleOutcome.passed(
                RULE_ID,
                RULE_NAME,
                String.format("Driver has %d min before mandatory rest (satisfies safeguard)", timeUntilRest.toMinutes())
        );
    }
}
