package org.logistix.model.graph;

import org.logistix.model.node.DecisionNode;
import org.logistix.model.node.NodeType;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Standard node implementation within a DecisionGraph.
 */
public record DecisionGraphNode(
        String nodeId,
        String name,
        NodeType nodeType,
        List<String> dependencies,
        Map<String, Object> properties,
        boolean optional
) implements DecisionNode {
    public DecisionGraphNode {
        Objects.requireNonNull(nodeId, "Node ID must not be null");
        Objects.requireNonNull(name, "Node name must not be null");
        Objects.requireNonNull(nodeType, "NodeType must not be null");
        dependencies = dependencies != null ? List.copyOf(dependencies) : Collections.emptyList();
        properties = properties != null ? Map.copyOf(properties) : Collections.emptyMap();
    }

    public static DecisionGraphNode of(String nodeId, String name, NodeType type) {
        return new DecisionGraphNode(nodeId, name, type, Collections.emptyList(), Collections.emptyMap(), false);
    }

    public static DecisionGraphNode of(String nodeId, String name, NodeType type, List<String> dependencies) {
        return new DecisionGraphNode(nodeId, name, type, dependencies, Collections.emptyMap(), false);
    }

    @Override public String getNodeId() { return nodeId; }
    @Override public String getName() { return name; }
    @Override public NodeType getNodeType() { return nodeType; }
    @Override public List<String> getDependencies() { return dependencies; }
    @Override public Map<String, Object> getProperties() { return properties; }
    @Override public boolean isOptional() { return optional; }
}
