package org.logistix.model.node;

import java.util.List;

/**
 * Node combining outputs from multiple concurrent upstream nodes.
 */
public interface AggregationNode extends DecisionNode {

    @Override
    default NodeType getNodeType() {
        return NodeType.AGGREGATION;
    }

    List<String> getSourceNodeIds();
}
