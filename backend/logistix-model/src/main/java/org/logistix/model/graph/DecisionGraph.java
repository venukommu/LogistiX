package org.logistix.model.graph;

import org.logistix.model.edge.DecisionEdge;
import org.logistix.model.metadata.ModelMetadata;
import org.logistix.model.model.AbstractDecisionModel;
import org.logistix.model.node.DecisionNode;
import org.logistix.model.strategy.StrategyType;
import org.logistix.model.variable.DecisionVariables;

import java.util.List;

/**
 * Graph-based DecisionModel topology supporting DAG, cyclic, and conditional branching executions.
 */
public class DecisionGraph extends AbstractDecisionModel {

    public DecisionGraph(
            String modelId,
            String name,
            ModelMetadata metadata,
            List<DecisionNode> nodes,
            List<DecisionEdge> edges,
            DecisionVariables variables
    ) {
        super(modelId, name, metadata, nodes, edges, variables, StrategyType.GRAPH);
    }

    public static DecisionGraphBuilder builder(String modelId) {
        return new DecisionGraphBuilder(modelId);
    }
}
