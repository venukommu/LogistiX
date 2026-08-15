package org.logistix.model.node;

import org.logistix.domain.constraint.ConstraintSeverity;

/**
 * Node evaluating feasibility guardrails and constraint boundaries.
 */
public interface ConstraintNode extends DecisionNode {

    @Override
    default NodeType getNodeType() {
        return NodeType.CONSTRAINT;
    }

    ConstraintSeverity getSeverity();
}
