package org.logistix.model.variable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Immutable container for strongly typed decision variables.
 */
public record DecisionVariables(
        Map<String, DecisionVariable<?>> variables
) {
    public DecisionVariables {
        variables = variables != null ? Map.copyOf(variables) : Collections.emptyMap();
    }

    public static DecisionVariables empty() {
        return new DecisionVariables(Collections.emptyMap());
    }

    public static DecisionVariables of(List<DecisionVariable<?>> variableList) {
        if (variableList == null || variableList.isEmpty()) {
            return empty();
        }
        Map<String, DecisionVariable<?>> map = new LinkedHashMap<>();
        variableList.forEach(v -> map.put(v.key(), v));
        return new DecisionVariables(map);
    }

    public Optional<DecisionVariable<?>> get(String key) {
        return Optional.ofNullable(variables.get(key));
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<DecisionVariable<T>> get(String key, Class<T> expectedType) {
        DecisionVariable<?> variable = variables.get(key);
        if (variable == null) {
            return Optional.empty();
        }
        return Optional.of((DecisionVariable<T>) variable);
    }

    public boolean contains(String key) {
        return variables.containsKey(key);
    }

    public int size() {
        return variables.size();
    }
}
