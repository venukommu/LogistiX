package org.logistix.model.edge;

/**
 * Directed relationship type connecting two DecisionNodes.
 */
public enum EdgeType {
    DEPENDS_ON,
    RUNS_BEFORE,
    RUNS_AFTER,
    CONDITIONAL,
    PARALLEL,
    RETRY,
    ON_FAILURE
}
