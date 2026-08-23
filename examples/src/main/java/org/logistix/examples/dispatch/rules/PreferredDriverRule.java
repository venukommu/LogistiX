package org.logistix.examples.dispatch.rules;

import org.logistix.domain.decision.DecisionContext;
import org.logistix.domain.rule.Rule;
import org.logistix.domain.rule.RuleOutcome;
import org.logistix.examples.dispatch.model.DispatchCandidate;
import org.logistix.examples.dispatch.model.DriverTier;

import java.util.Map;

/**
 * Deterministic business rule providing positive score bonus for high-tier / preferred partner drivers.
 */
public class PreferredDriverRule implements Rule<DispatchCandidate> {

    public static final String RULE_ID = "RULE_PREFERRED_DRIVER";
    public static final String RULE_NAME = "Preferred Driver Tier Incentive";

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
        return 100;
    }

    @Override
    public RuleOutcome evaluate(DispatchCandidate candidate, DecisionContext context) {
        DriverTier tier = candidate.driver().tier();
        double bonus = switch (tier) {
            case PLATINUM -> 0.15;
            case GOLD -> 0.10;
            case SILVER -> 0.05;
            case STANDARD -> 0.0;
        };

        if (bonus > 0.0) {
            return new RuleOutcome(
                    RULE_ID,
                    RULE_NAME,
                    true,
                    String.format("Driver '%s' holds %s tier (awarded +%.2f score bonus)", candidate.driver().name(), tier, bonus),
                    bonus,
                    Map.of("tier", tier.name(), "scoreBonus", bonus)
            );
        }

        return RuleOutcome.passed(RULE_ID, RULE_NAME, "Standard tier driver (no tier adjustment)");
    }
}
