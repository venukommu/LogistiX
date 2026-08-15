package org.logistix.decision.pipeline;

/**
 * Individual pipeline execution step contract.
 *
 * @param <C> Candidate type
 */
public interface PipelineStage<C> {

    String getStageName();

    int getOrder();

    PipelineExecutionContext<C> process(PipelineExecutionContext<C> executionContext);
}
