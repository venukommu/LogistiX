package org.logistix.engine.engine;

import org.logistix.domain.decision.DecisionContext;
import org.logistix.domain.decision.DecisionResult;
import org.logistix.engine.context.LogistiXContext;
import org.logistix.engine.executor.DecisionExecutor;
import org.logistix.engine.registry.DecisionRegistry;

/**
 * Top-level runtime interface coordinating engine startup, registry management, and execution orchestration.
 */
public interface DecisionEngineRuntime extends EngineLifecycle {

    LogistiXContext getContext();

    DecisionExecutor getExecutor();

    DecisionRegistry getRegistry();

    <T> DecisionResult<T> executeDecision(DecisionContext context);
}
