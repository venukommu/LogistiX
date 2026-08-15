package org.logistix.domain.exceptions;

/**
 * Thrown when no registered decision engine supports a requested decision type.
 */
public class EngineNotFoundException extends DecisionException {

    private final String decisionType;

    public EngineNotFoundException(String decisionType) {
        super(String.format("No decision engine registered for decision type '%s'", decisionType), "ENGINE_NOT_FOUND");
        this.decisionType = decisionType;
    }

    public String getDecisionType() {
        return decisionType;
    }
}
