package org.logistix.model.validation;

import java.util.Collections;
import java.util.List;

/**
 * Immutable outcome of a model validation pass.
 */
public record ValidationResult(
        boolean isValid,
        List<ValidationError> errors,
        List<ValidationError> warnings
) {
    public ValidationResult {
        errors = errors != null ? List.copyOf(errors) : Collections.emptyList();
        warnings = warnings != null ? List.copyOf(warnings) : Collections.emptyList();
    }

    public static ValidationResult ok() {
        return new ValidationResult(true, Collections.emptyList(), Collections.emptyList());
    }

    public static ValidationResult failures(List<ValidationError> errors, List<ValidationError> warnings) {
        return new ValidationResult(errors == null || errors.isEmpty(), errors, warnings);
    }
}
