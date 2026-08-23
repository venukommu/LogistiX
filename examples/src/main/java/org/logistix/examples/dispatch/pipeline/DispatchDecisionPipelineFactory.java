package org.logistix.examples.dispatch.pipeline;

import org.logistix.domain.ports.AIProvider;
import org.logistix.dsl.LogistiX;
import org.logistix.engine.pipeline.DecisionPipeline;
import org.logistix.examples.dispatch.ai.DispatchAIAdvisor;
import org.logistix.examples.dispatch.ai.DriverDispatchAIStep;
import org.logistix.examples.dispatch.constraints.DriverDispatchConstraintStep;
import org.logistix.examples.dispatch.recommendation.DriverDispatchRecommendationStep;
import org.logistix.examples.dispatch.rules.DriverDispatchRuleStep;
import org.logistix.examples.dispatch.scoring.DriverDispatchScoringStep;

/**
 * Factory constructing executable DecisionPipeline instances for AI-Assisted Driver Dispatch.
 */
public final class DispatchDecisionPipelineFactory {

    public static final String DECISION_TYPE = "driver-dispatch";

    private DispatchDecisionPipelineFactory() {}

    /**
     * Creates a deterministic (rules + constraints + scoring + recommendation) pipeline.
     */
    public static DecisionPipeline createDeterministicPipeline() {
        return LogistiX.pipelineBuilder(DECISION_TYPE)
                .name("Deterministic Driver Dispatch Pipeline")
                .step(new DriverDispatchConstraintStep())
                .step(new DriverDispatchRuleStep())
                .step(new DriverDispatchScoringStep())
                .step(new DriverDispatchRecommendationStep())
                .build();
    }

    /**
     * Creates a hybrid AI-assisted pipeline with contextual reasoning and graceful fallback.
     */
    public static DecisionPipeline createHybridAiPipeline() {
        return createHybridAiPipeline(new DispatchAIAdvisor());
    }

    /**
     * Creates a hybrid AI-assisted pipeline using a specific AIProvider SPI implementation.
     */
    public static DecisionPipeline createHybridAiPipeline(AIProvider aiProvider) {
        return LogistiX.pipelineBuilder(DECISION_TYPE)
                .name("AI-Assisted Hybrid Driver Dispatch Pipeline")
                .step(new DriverDispatchConstraintStep())
                .step(new DriverDispatchRuleStep())
                .step(new DriverDispatchScoringStep())
                .step(new DriverDispatchAIStep(aiProvider))
                .step(new DriverDispatchRecommendationStep())
                .build();
    }
}
