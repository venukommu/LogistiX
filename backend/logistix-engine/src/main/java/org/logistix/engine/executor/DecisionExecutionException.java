package org.logistix.engine.executor;

import org.logistix.domain.exceptions.DecisionException;

/**
 * Thrown when an unrecoverable failure occurs during decision pipeline execution.
 */
public class DecisionExecutionException extends DecisionException {

    private final String stepId;

    public DecisionExecutionException(String message) {
        super(message, "EXECUTION_ERROR");
        this.stepId = null;
    }

    public DecisionExecutionException(String message, Throwable cause) {
        super(message, "EXECUTION_ERROR", cause);
        this.stepId = null;
    }

    public DecisionExecutionException(String stepId, String message) {
        super(String.format("Step '%s' failed: %s", stepId, message), "STEP_EXECUTION_ERROR");
        this.stepId = stepId;
    }

    public DecisionExecutionException(String stepId, String message, Throwable cause) {
        super(String.format("Step '%s' failed: %s", stepId, message), "STEP_EXECUTION_ERROR", cause);
        this.stepId = stepId;
    }

    public String getStepId() {
        return stepId;
    }
}
