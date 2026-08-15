package org.logistix.engine.steps;

/**
 * Pipeline step contract responsible for evaluating business compliance, operational policies, and rule outcomes.
 */
public interface RuleStep extends DecisionStep {

    default String getStepType() {
        return "RULE_STEP";
    }
}
