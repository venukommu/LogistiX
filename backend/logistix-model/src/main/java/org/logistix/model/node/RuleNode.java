package org.logistix.model.node;

/**
 * Node evaluating business compliance rules and determining scoring adjustments.
 */
public interface RuleNode extends DecisionNode {

    @Override
    default NodeType getNodeType() {
        return NodeType.RULE;
    }

    int getPriority();
}
