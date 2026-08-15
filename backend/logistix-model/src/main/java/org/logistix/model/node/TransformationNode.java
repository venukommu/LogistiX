package org.logistix.model.node;

import java.time.Duration;

/**
 * Node transforming facts, shaping data schemas, or computing derived state attributes.
 */
public interface TransformationNode extends DecisionNode {

    @Override
    default NodeType getNodeType() {
        return NodeType.TRANSFORMATION;
    }

    String getOutputFactKey();
}
