package org.logistix.model.state;

import org.logistix.domain.fact.FactBag;
import org.logistix.model.variable.DecisionVariables;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Immutable operational state representing snapshot data, fact values, intermediate node outputs,
 * and errors accumulated across execution.
 */
public record DecisionState(
        UUID stateId,
        UUID contextId,
        FactBag facts,
        Map<String, Object> intermediateResults,
        Map<String, Object> nodeOutputs,
        DecisionVariables variables,
        List<String> warnings,
        List<String> errors,
        Instant timestamp
) {
    public DecisionState {
        Objects.requireNonNull(stateId, "State ID must not be null");
        Objects.requireNonNull(contextId, "Context ID must not be null");
        facts = facts != null ? facts : FactBag.empty();
        intermediateResults = intermediateResults != null ? Map.copyOf(intermediateResults) : Collections.emptyMap();
        nodeOutputs = nodeOutputs != null ? Map.copyOf(nodeOutputs) : Collections.emptyMap();
        variables = variables != null ? variables : DecisionVariables.empty();
        warnings = warnings != null ? List.copyOf(warnings) : Collections.emptyList();
        errors = errors != null ? List.copyOf(errors) : Collections.emptyList();
        timestamp = timestamp != null ? timestamp : Instant.now();
    }

    public static DecisionState initial(UUID contextId, FactBag facts) {
        return new DecisionState(
                UUID.randomUUID(),
                contextId,
                facts,
                Collections.emptyMap(),
                Collections.emptyMap(),
                DecisionVariables.empty(),
                Collections.emptyList(),
                Collections.emptyList(),
                Instant.now()
        );
    }

    public Optional<Object> getNodeOutput(String nodeId) {
        return Optional.ofNullable(nodeOutputs.get(nodeId));
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<T> getNodeOutput(String nodeId, Class<T> targetType) {
        Object out = nodeOutputs.get(nodeId);
        if (out != null && targetType.isInstance(out)) {
            return Optional.of((T) out);
        }
        return Optional.empty();
    }
}
