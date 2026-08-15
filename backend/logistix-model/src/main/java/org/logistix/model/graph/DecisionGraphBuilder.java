package org.logistix.model.graph;

import org.logistix.model.edge.DecisionEdge;
import org.logistix.model.metadata.ModelMetadata;
import org.logistix.model.node.DecisionNode;
import org.logistix.model.variable.DecisionVariable;
import org.logistix.model.variable.DecisionVariables;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Fluent builder for constructing immutable DecisionGraphs.
 */
public class DecisionGraphBuilder {

    private final String modelId;
    private String name;
    private ModelMetadata metadata;
    private final List<DecisionNode> nodes = new ArrayList<>();
    private final List<DecisionEdge> edges = new ArrayList<>();
    private final List<DecisionVariable<?>> variables = new ArrayList<>();

    public DecisionGraphBuilder(String modelId) {
        this.modelId = Objects.requireNonNull(modelId, "Model ID cannot be null");
        this.name = modelId + "-Graph";
    }

    public DecisionGraphBuilder name(String name) {
        this.name = Objects.requireNonNull(name, "Name cannot be null");
        return this;
    }

    public DecisionGraphBuilder metadata(ModelMetadata metadata) {
        this.metadata = metadata;
        return this;
    }

    public DecisionGraphBuilder addNode(DecisionNode node) {
        this.nodes.add(Objects.requireNonNull(node, "DecisionNode cannot be null"));
        return this;
    }

    public DecisionGraphBuilder addEdge(DecisionEdge edge) {
        this.edges.add(Objects.requireNonNull(edge, "DecisionEdge cannot be null"));
        return this;
    }

    public DecisionGraphBuilder addEdge(String sourceNodeId, String targetNodeId) {
        this.edges.add(DecisionEdge.sequence(sourceNodeId, targetNodeId));
        return this;
    }

    public DecisionGraphBuilder addConditionalEdge(String sourceNodeId, String targetNodeId, String conditionExpression) {
        this.edges.add(DecisionEdge.conditional(sourceNodeId, targetNodeId, conditionExpression));
        return this;
    }

    public DecisionGraphBuilder addVariable(DecisionVariable<?> variable) {
        this.variables.add(Objects.requireNonNull(variable, "Variable cannot be null"));
        return this;
    }

    public DecisionGraph build() {
        ModelMetadata effectiveMetadata = (this.metadata != null)
                ? this.metadata
                : ModelMetadata.simple("1.0.0", this.name);

        return new DecisionGraph(
                this.modelId,
                this.name,
                effectiveMetadata,
                this.nodes,
                this.edges,
                DecisionVariables.of(this.variables)
        );
    }
}
