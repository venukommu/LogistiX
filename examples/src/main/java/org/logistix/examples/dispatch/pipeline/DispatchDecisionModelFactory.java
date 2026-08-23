package org.logistix.examples.dispatch.pipeline;

import org.logistix.dsl.LogistiX;
import org.logistix.model.graph.DecisionGraph;
import org.logistix.model.graph.DecisionGraphNode;
import org.logistix.model.metadata.ModelMetadata;
import org.logistix.model.node.NodeType;
import org.logistix.model.variable.DecisionVariable;

import java.util.List;

/**
 * Factory constructing declarative DecisionGraph models for Driver Dispatch topology visualization and execution.
 */
public final class DispatchDecisionModelFactory {

    public static final String MODEL_ID = "model-driver-dispatch";

    private DispatchDecisionModelFactory() {}

    /**
     * Constructs the declarative DecisionGraph topology for Driver Dispatch.
     */
    public static DecisionGraph createModel() {
        return LogistiX.graph(MODEL_ID)
                .name("Driver Dispatch Decision Topology")
                .metadata(ModelMetadata.simple(
                        "1.0.0",
                        "AI-Assisted Dispatch Decision Graph with hard feasibility pruning and multi-criteria scoring"
                ))
                .addNode(DecisionGraphNode.of("node-constraints", "Feasibility Constraints", NodeType.CONSTRAINT))
                .addNode(DecisionGraphNode.of("node-rules", "Business Rules", NodeType.RULE, List.of("node-constraints")))
                .addNode(DecisionGraphNode.of("node-scoring", "Multi-Criteria Scoring", NodeType.SCORING, List.of("node-rules")))
                .addNode(new DecisionGraphNode("node-ai", "AI Contextual Advisor", NodeType.AI, List.of("node-scoring"), java.util.Collections.emptyMap(), true))
                .addNode(DecisionGraphNode.of("node-recommendation", "Recommendation & Explainability", NodeType.RECOMMENDATION, List.of("node-scoring", "node-ai")))
                .addEdge("node-constraints", "node-rules")
                .addEdge("node-rules", "node-scoring")
                .addConditionalEdge("node-scoring", "node-ai", "context.getEnvironmentAttribute('aiEnabled', Boolean.class).orElse(true)")
                .addEdge("node-ai", "node-recommendation")
                .addConditionalEdge("node-scoring", "node-recommendation", "!context.getEnvironmentAttribute('aiEnabled', Boolean.class).orElse(true)")
                .addVariable(DecisionVariable.of("maxDeadheadKm", org.logistix.model.variable.VariableType.NUMBER, 250.0))
                .addVariable(DecisionVariable.of("optimalBufferMinutes", org.logistix.model.variable.VariableType.NUMBER, 120))
                .addVariable(DecisionVariable.of("aiEnabled", org.logistix.model.variable.VariableType.BOOLEAN, true))
                .build();
    }
}
