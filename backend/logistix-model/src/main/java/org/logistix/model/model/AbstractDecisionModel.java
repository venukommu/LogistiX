package org.logistix.model.model;

import org.logistix.model.edge.DecisionEdge;
import org.logistix.model.metadata.ModelMetadata;
import org.logistix.model.node.DecisionNode;
import org.logistix.model.strategy.StrategyType;
import org.logistix.model.variable.DecisionVariables;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Skeletal base implementation of DecisionModel.
 */
public abstract class AbstractDecisionModel implements DecisionModel {

    private final String modelId;
    private final String name;
    private final ModelMetadata metadata;
    private final List<DecisionNode> nodes;
    private final List<DecisionEdge> edges;
    private final DecisionVariables variables;
    private final StrategyType preferredStrategy;

    protected AbstractDecisionModel(
            String modelId,
            String name,
            ModelMetadata metadata,
            List<DecisionNode> nodes,
            List<DecisionEdge> edges,
            DecisionVariables variables,
            StrategyType preferredStrategy
    ) {
        this.modelId = Objects.requireNonNull(modelId, "Model ID must not be null");
        this.name = Objects.requireNonNull(name, "Name must not be null");
        this.metadata = metadata != null ? metadata : ModelMetadata.simple("1.0.0", name);
        this.nodes = nodes != null ? List.copyOf(nodes) : Collections.emptyList();
        this.edges = edges != null ? List.copyOf(edges) : Collections.emptyList();
        this.variables = variables != null ? variables : DecisionVariables.empty();
        this.preferredStrategy = preferredStrategy != null ? preferredStrategy : StrategyType.SEQUENTIAL;
    }

    @Override public String getModelId() { return modelId; }
    @Override public String getName() { return name; }
    @Override public ModelMetadata getMetadata() { return metadata; }
    @Override public List<DecisionNode> getNodes() { return nodes; }
    @Override public List<DecisionEdge> getEdges() { return edges; }
    @Override public DecisionVariables getVariables() { return variables; }
    @Override public StrategyType getPreferredStrategy() { return preferredStrategy; }
}
