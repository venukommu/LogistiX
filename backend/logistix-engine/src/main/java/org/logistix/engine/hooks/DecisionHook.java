package org.logistix.engine.hooks;

import org.logistix.domain.decision.DecisionContext;
import org.logistix.domain.decision.DecisionResult;
import org.logistix.engine.steps.DecisionStep;
import org.logistix.engine.steps.StepResult;

/**
 * Extension point contract for intercepting decision execution lifecycle events.
 */
public interface DecisionHook {

    default void beforeDecision(DecisionContext context) {}

    default void afterDecision(DecisionContext context, DecisionResult<?> result) {}

    default void beforeStep(DecisionContext context, DecisionStep step) {}

    default void afterStep(DecisionContext context, DecisionStep step, StepResult result) {}

    default void onDecisionCompleted(DecisionResult<?> result) {}

    default void onDecisionFailed(DecisionContext context, Throwable error) {}

    default int getOrder() {
        return 0;
    }
}
