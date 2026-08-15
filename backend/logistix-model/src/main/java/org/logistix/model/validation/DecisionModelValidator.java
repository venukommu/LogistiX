package org.logistix.model.validation;

import org.logistix.model.model.DecisionModel;

/**
 * Top-level validation contract for DecisionModels.
 */
public interface DecisionModelValidator {

    ValidationResult validate(DecisionModel model);
}
