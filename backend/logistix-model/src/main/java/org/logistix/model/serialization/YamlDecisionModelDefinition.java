package org.logistix.model.serialization;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Declarative Schema DTO representing a DecisionModel loaded from a YAML specification.
 *
 * <pre>{@code
 * decision:
 *   name: dispatch
 *   strategy: graph
 *   version: 1.0.0
 *   nodes:
 *     - id: hos-check
 *       type: CONSTRAINT
 *     - id: weather-risk
 *       type: AI
 *     - id: traffic-eta
 *       type: AI
 *     - id: recommendation
 *       type: RECOMMENDATION
 * }</pre>
 */
public record YamlDecisionModelDefinition(
        String name,
        String strategy,
        String version,
        String description,
        List<YamlNodeDefinition> nodes,
        List<YamlEdgeDefinition> edges,
        Map<String, Object> variables
) {
    public YamlDecisionModelDefinition {
        Objects.requireNonNull(name, "Decision name must not be null");
        strategy = strategy != null ? strategy : "sequential";
        version = version != null ? version : "1.0.0";
        description = description != null ? description : "";
        nodes = nodes != null ? List.copyOf(nodes) : Collections.emptyList();
        edges = edges != null ? List.copyOf(edges) : Collections.emptyList();
        variables = variables != null ? Map.copyOf(variables) : Collections.emptyMap();
    }

    public record YamlNodeDefinition(
            String id,
            String name,
            String type,
            List<String> dependencies,
            Map<String, Object> properties
    ) {}

    public record YamlEdgeDefinition(
            String source,
            String target,
            String type,
            String condition
    ) {}
}
