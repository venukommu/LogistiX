package org.logistix.examples.dispatch.rules;

import org.logistix.domain.decision.DecisionContext;
import org.logistix.domain.rule.Rule;
import org.logistix.domain.rule.RuleOutcome;
import org.logistix.examples.dispatch.model.DispatchCandidate;

import java.util.Map;

/**
 * Deterministic business rule awarding a score bonus if the shipment destination or origin
 * matches the driver's home operating region, improving backhaul utilization.
 */
public class RegionalAffinityRule implements Rule<DispatchCandidate> {

    public static final String RULE_ID = "RULE_REGIONAL_AFFINITY";
    public static final String RULE_NAME = "Driver Regional Affinity & Backhaul Incentive";

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
        return 80;
    }

    @Override
    public RuleOutcome evaluate(DispatchCandidate candidate, DecisionContext context) {
        String home = candidate.driver().homeRegion();
        String destRegion = candidate.shipment().destinationRegion();

        if (home != null && !home.equalsIgnoreCase("UNKNOWN") && home.equalsIgnoreCase(destRegion)) {
            double bonus = 0.08;
            return new RuleOutcome(
                    RULE_ID,
                    RULE_NAME,
                    true,
                    String.format("Destination region '%s' matches driver home region '%s' (+%.2f bonus)", destRegion, home, bonus),
                    bonus,
                    Map.of("homeRegion", home, "destinationRegion", destRegion, "bonus", bonus)
            );
        }

        return RuleOutcome.passed(RULE_ID, RULE_NAME, "No regional affinity match");
    }
}
