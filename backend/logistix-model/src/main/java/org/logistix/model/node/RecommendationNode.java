package org.logistix.model.node;

/**
 * Node synthesizing candidate ranking and explainability summaries into final recommendations.
 */
public interface RecommendationNode extends DecisionNode {

    @Override
    default NodeType getNodeType() {
        return NodeType.RECOMMENDATION;
    }

    int getTopK();

    boolean isExplainabilityRequired();
}
