package org.logistix.model.node;

import java.util.Map;

/**
 * Node computing normalized multi-criteria objective scores.
 */
public interface ScoringNode extends DecisionNode {

    @Override
    default NodeType getNodeType() {
        return NodeType.SCORING;
    }

    Map<String, Double> getCriteriaWeights();
}
