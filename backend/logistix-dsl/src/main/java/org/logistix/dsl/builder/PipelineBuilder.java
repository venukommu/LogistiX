package org.logistix.dsl.builder;

import org.logistix.engine.builder.DecisionPipelineBuilder;
import org.logistix.engine.pipeline.DecisionPipeline;
import org.logistix.engine.steps.DecisionStep;

import java.util.List;
import java.util.UUID;

/**
 * Fluent builder for creating immutable DecisionPipelines.
 */
public class PipelineBuilder {

    private final DecisionPipelineBuilder delegate;

    public PipelineBuilder(String decisionType) {
        this.delegate = DecisionPipeline.builder(decisionType);
    }

    public static PipelineBuilder of(String decisionType) {
        return new PipelineBuilder(decisionType);
    }

    public PipelineBuilder pipelineId(UUID pipelineId) {
        this.delegate.pipelineId(pipelineId);
        return this;
    }

    public PipelineBuilder name(String name) {
        this.delegate.name(name);
        return this;
    }

    public PipelineBuilder version(String version) {
        this.delegate.version(version);
        return this;
    }

    public PipelineBuilder step(DecisionStep step) {
        this.delegate.step(step);
        return this;
    }

    public PipelineBuilder steps(List<DecisionStep> steps) {
        this.delegate.steps(steps);
        return this;
    }

    public PipelineBuilder withMetadata(String key, Object value) {
        this.delegate.withMetadata(key, value);
        return this;
    }

    public DecisionPipeline build() {
        return this.delegate.build();
    }
}
