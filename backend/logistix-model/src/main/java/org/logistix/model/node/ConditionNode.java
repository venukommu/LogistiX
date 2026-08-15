package org.logistix.model.node;

import java.time.Duration;

/**
 * Node performing boolean branch evaluation to control downstream graph edge traversal.
 */
public interface ConditionNode extends DecisionNode {

    @Override
    default NodeType getNodeType() {
        return NodeType.CONDITION;
    }

    String getExpression();

    String getTrueTargetNodeId();

    String getFalseTargetNodeId();
}
