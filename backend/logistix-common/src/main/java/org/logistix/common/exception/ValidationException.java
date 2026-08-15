package org.logistix.common.exception;

import java.util.Collections;
import java.util.List;

/**
 * Thrown when input validation fails before domain processing.
 */
public class ValidationException extends LogistixException {

    private final List<String> validationErrors;

    public ValidationException(String message) {
        this(message, Collections.singletonList(message));
    }

    public ValidationException(String message, List<String> validationErrors) {
        super(message, "VALIDATION_FAILED");
        this.validationErrors = validationErrors != null ? List.copyOf(validationErrors) : Collections.emptyList();
    }

    public List<String> getValidationErrors() {
        return validationErrors;
    }
}
