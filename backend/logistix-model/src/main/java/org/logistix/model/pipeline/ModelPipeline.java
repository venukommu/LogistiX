package org.logistix.model.pipeline;

import org.logistix.model.edge.DecisionEdge;
import org.logistix.model.metadata.ModelMetadata;
import org.logistix.model.model.AbstractDecisionModel;
import org.logistix.model.node.DecisionNode;
import org.logistix.model.strategy.StrategyType;
import org.logistix.model.variable.DecisionVariables;

import java.util.ArrayList;
import java.util.List;

/**
 * Sequential linear pipeline implementation of DecisionModel.
 *
 * <p>In LogistiX Decision Intelligence Platform, the pipeline is one specialized linear topology
 * implementation among graph, agentic, and parallel decision models.</p>
 */
public class ModelPipeline extends AbstractDecisionModel {

    public ModelPipeline(
            String modelId,
            String name,
            ModelMetadata metadata,
            List<DecisionNode> nodes,
            DecisionVariables variables
    ) {
        super(modelId, name, metadata, nodes, generateSequentialEdges(nodes), variables, StrategyType.SEQUENTIAL);
    }

    private static List<DecisionEdge> generateSequentialEdges(List<DecisionNode> nodes) {
        if (nodes == null || nodes.size() < 2) {
            return List.of();
        }
        List<DecisionEdge> edges = new ArrayList<>();
        for (int i = 0; i < nodes.size() - 1; i++) {
            edges.add(DecisionEdge.sequence(nodes.get(i).getNodeId(), nodes.get(i + 1).getNodeId()));
        }
        return List.copyOf(edges);
    }
}
