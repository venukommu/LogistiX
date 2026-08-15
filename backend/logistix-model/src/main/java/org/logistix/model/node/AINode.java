package org.logistix.model.node;

/**
 * Node executing AI/LLM model inference, prompt templates, or semantic reasoning.
 */
public interface AINode extends DecisionNode {

    @Override
    default NodeType getNodeType() {
        return NodeType.AI;
    }

    String getModelId();

    double getTemperature();
}
