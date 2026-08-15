package org.logistix.model.validation;

import org.logistix.model.graph.DecisionGraph;

/**
 * Validates graph topologies for cycles, isolated orphaned nodes, and missing edge endpoints.
 */
public interface DecisionGraphValidator {

    ValidationResult validateGraph(DecisionGraph graph);
}
