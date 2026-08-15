package org.logistix.domain.exceptions;

/**
 * Thrown when an unrecoverable failure occurs during rule evaluation.
 */
public class RuleExecutionException extends DecisionException {

    private final String ruleId;

    public RuleExecutionException(String ruleId, String message) {
        super(String.format("Error executing rule '%s': %s", ruleId, message), "RULE_EXECUTION_ERROR");
        this.ruleId = ruleId;
    }

    public RuleExecutionException(String ruleId, String message, Throwable cause) {
        super(String.format("Error executing rule '%s': %s", ruleId, message), "RULE_EXECUTION_ERROR", cause);
        this.ruleId = ruleId;
    }

    public String getRuleId() {
        return ruleId;
    }
}
