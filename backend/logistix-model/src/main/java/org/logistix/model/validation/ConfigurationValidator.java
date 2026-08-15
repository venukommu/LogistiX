package org.logistix.model.validation;

import java.util.Map;

/**
 * Validates dynamic configuration maps and runtime environment properties.
 */
public interface ConfigurationValidator {

    ValidationResult validateConfiguration(Map<String, Object> properties);
}
