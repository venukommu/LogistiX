package org.logistix.model.strategy;

import org.logistix.model.model.DecisionModel;
import org.logistix.model.plan.ExecutionPlan;

/**
 * Strategy pattern interface for compiling a declarative DecisionModel
 * into an executable ExecutionPlan.
 */
public interface ExecutionStrategy {

    StrategyType getStrategyType();

    ExecutionPlan plan(DecisionModel model);
}
