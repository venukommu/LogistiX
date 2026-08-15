package org.logistix.decision.pipeline;

import org.logistix.domain.decision.DecisionResult;

import java.util.List;

/**
 * Composite orchestrator executing an ordered series of pipeline stages.
 *
 * @param <C> Candidate type
 * @param <R> Recommendation result type
 */
public interface DecisionPipeline<C, R> {

    List<PipelineStage<C>> getStages();

    DecisionResult<R> execute(PipelineExecutionContext<C> initialContext);
}
