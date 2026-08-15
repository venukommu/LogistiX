package org.logistix.engine.executor;

import org.logistix.domain.decision.DecisionContext;
import org.logistix.domain.decision.DecisionRequest;
import org.logistix.domain.decision.DecisionResponse;
import org.logistix.domain.decision.DecisionResult;
import org.logistix.engine.pipeline.DecisionPipeline;

/**
 * Primary engine executor contract responsible for running decision pipelines,
 * invoking lifecycle hooks, accumulating metrics, capturing trace logs, and constructing DecisionResults.
 */
public interface DecisionExecutor {

    <T> DecisionResult<T> execute(DecisionPipeline pipeline, DecisionContext context);

    <T> DecisionResult<T> execute(String decisionType, DecisionContext context);

    <C, R> DecisionResponse<R> executeRequest(DecisionRequest<C> request);
}
