package org.logistix.model.node;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Fundamental atomic unit of computation in a DecisionModel.
 * Represents a declarative task agnostic of underlying frameworks or business logic.
 */
public interface DecisionNode {

    String getNodeId();

    String getName();

    NodeType getNodeType();

    default List<String> getDependencies() {
        return Collections.emptyList();
    }

    default Map<String, Object> getProperties() {
        return Collections.emptyMap();
    }

    default boolean isOptional() {
        return false;
    }
}
