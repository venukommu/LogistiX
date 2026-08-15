package org.logistix.examples;

import org.logistix.domain.decision.DecisionContext;
import org.logistix.domain.rule.Rule;
import org.logistix.domain.rule.RuleOutcome;
import org.logistix.dsl.annotation.DecisionRule;

/**
 * <h3>Custom Rule Example</h3>
 * Demonstrates defining a declarative and programmatic business policy rule.
 */
@DecisionRule(id = "RULE-PREMIUM-CARRIER", name = "Boost Tier-1 Carrier Priority", priority = 10, appliesTo = {"carrier-recommendation"})
public class CustomRuleExample implements Rule<CustomRuleExample.CarrierCandidate> {

    public record CarrierCandidate(String carrierId, String tier, double baseScore) {}

    @Override
    public String getId() {
        return "RULE-PREMIUM-CARRIER";
    }

    @Override
    public String getName() {
        return "Boost Tier-1 Carrier Priority";
    }

    @Override
    public int getPriority() {
        return 10;
    }

    @Override
    public RuleOutcome evaluate(CarrierCandidate candidate, DecisionContext context) {
        if ("TIER_1".equalsIgnoreCase(candidate.tier())) {
            return RuleOutcome.passed(
                    "RULE-PREMIUM-CARRIER",
                    "Boost Tier-1 Carrier Priority",
                    "Tier-1 carrier qualifies for premium priority bonus",
                    0.15
            );
        }
        return RuleOutcome.passed(
                "RULE-PREMIUM-CARRIER",
                "Boost Tier-1 Carrier Priority",
                "Non-tier-1 carrier evaluated without bonus"
        );
    }
}
