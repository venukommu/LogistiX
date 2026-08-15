package org.logistix.dsl.fluent;

import org.logistix.engine.builder.DecisionPipelineBuilder;
import org.logistix.engine.pipeline.DecisionPipeline;
import org.logistix.engine.steps.DecisionStep;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Fluent API for constructing DecisionPipelines with zero boilerplate.
 *
 * <pre>{@code
 * DecisionPipeline pipeline = LogistiX.pipeline("driver-dispatch")
 *     .step(new DistanceConstraintStep())
 *     .step(new SeniorityRuleStep())
 *     .step(new AiInferenceStep())
 *     .step(new MultiCriteriaScoringStep())
 *     .step(new RecommendationSynthesisStep())
 *     .build();
 * }</pre>
 */
public class FluentPipeline {

    private final DecisionPipelineBuilder builder;

    public FluentPipeline(String decisionType) {
        this.builder = DecisionPipeline.builder(decisionType);
    }

    public static FluentPipeline of(String decisionType) {
        return new FluentPipeline(decisionType);
    }

    public FluentPipeline id(UUID pipelineId) {
        this.builder.pipelineId(pipelineId);
        return this;
    }

    public FluentPipeline name(String name) {
        this.builder.name(name);
        return this;
    }

    public FluentPipeline version(String version) {
        this.builder.version(version);
        return this;
    }

    public FluentPipeline step(DecisionStep step) {
        this.builder.step(step);
        return this;
    }

    public FluentPipeline steps(List<DecisionStep> steps) {
        this.builder.steps(steps);
        return this;
    }

    public FluentPipeline metadata(String key, Object value) {
        this.builder.withMetadata(key, value);
        return this;
    }

    public DecisionPipeline build() {
        return this.builder.build();
    }
}
