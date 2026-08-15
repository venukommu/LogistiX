package org.logistix.model.variable;

import java.util.Objects;
import java.util.Optional;

/**
 * Strongly typed variable definition within a DecisionModel.
 *
 * @param <T> Payload type
 */
public record DecisionVariable<T>(
        String key,
        VariableType type,
        T defaultValue,
        String description,
        boolean required
) {
    public DecisionVariable {
        Objects.requireNonNull(key, "Variable key must not be null");
        Objects.requireNonNull(type, "Variable type must not be null");
        description = description != null ? description : "";
    }

    public static <T> DecisionVariable<T> of(String key, VariableType type, T defaultValue) {
        return new DecisionVariable<>(key, type, defaultValue, "", false);
    }

    public static <T> DecisionVariable<T> required(String key, VariableType type, String description) {
        return new DecisionVariable<>(key, type, null, description, true);
    }

    public Optional<T> getDefaultValue() {
        return Optional.ofNullable(defaultValue);
    }
}
