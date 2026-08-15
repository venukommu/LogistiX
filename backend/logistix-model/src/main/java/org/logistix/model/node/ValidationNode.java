package org.logistix.model.node;

import java.util.List;

/**
 * Node performing input verification, fact schema validation, or business invariant checks.
 */
public interface ValidationNode extends DecisionNode {

    @Override
    default NodeType getNodeType() {
        return NodeType.VALIDATION;
    }

    List<String> getRequiredFactKeys();
}
