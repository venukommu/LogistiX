package org.logistix.domain.exceptions;

import org.logistix.domain.constraint.ConstraintViolation;

import java.util.Collections;
import java.util.List;

/**
 * Thrown when a hard constraint violation aborts decision processing.
 */
public class ConstraintViolationException extends DecisionException {

    private final List<ConstraintViolation> violations;

    public ConstraintViolationException(String message, List<ConstraintViolation> violations) {
        super(message, "CONSTRAINT_VIOLATION");
        this.violations = violations != null ? List.copyOf(violations) : Collections.emptyList();
    }

    public List<ConstraintViolation> getViolations() {
        return violations;
    }
}
