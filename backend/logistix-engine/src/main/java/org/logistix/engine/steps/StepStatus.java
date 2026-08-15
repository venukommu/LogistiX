package org.logistix.engine.steps;

/**
 * Execution outcome status of an individual pipeline step.
 */
public enum StepStatus {
    SUCCESS,
    SKIPPED,
    FAILED,
    SHORT_CIRCUIT
}
