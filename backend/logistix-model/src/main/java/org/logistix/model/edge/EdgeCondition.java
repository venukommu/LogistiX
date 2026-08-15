package org.logistix.model.edge;

import org.logistix.model.state.DecisionState;

/**
 * Predicate governing whether a conditional edge should be traversed.
 */
@FunctionalInterface
public interface EdgeCondition {

    boolean test(DecisionState state);

    static EdgeCondition alwaysTrue() {
        return state -> true;
    }
}
