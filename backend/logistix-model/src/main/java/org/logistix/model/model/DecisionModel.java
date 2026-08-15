package org.logistix.model.model;

import org.logistix.model.edge.DecisionEdge;
import org.logistix.model.metadata.ModelMetadata;
import org.logistix.model.node.DecisionNode;
import org.logistix.model.strategy.StrategyType;
import org.logistix.model.variable.DecisionVariables;

import java.util.List;
import java.util.Optional;

/**
 * Top-level declarative contract describing WHAT should execute in a decision workflow.
 * Encapsulates nodes, relationship edges, variables, and version metadata while remaining
 * agnostic to the underlying execution runtime strategy.
 */
public interface DecisionModel {

    String getModelId();

    String getName();

    ModelMetadata getMetadata();

    List<DecisionNode> getNodes();

    List<DecisionEdge> getEdges();

    DecisionVariables getVariables();

    StrategyType getPreferredStrategy();

    default Optional<DecisionNode> findNode(String nodeId) {
        return getNodes().stream().filter(n -> n.getNodeId().equals(nodeId)).findFirst();
    }
}
