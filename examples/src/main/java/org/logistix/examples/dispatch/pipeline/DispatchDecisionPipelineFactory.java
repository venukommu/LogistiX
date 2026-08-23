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
 * Factory constructing executable DecisionPipeline instances for commercial driver dispatch in multiple execution modes.
 */
public final class DispatchDecisionPipelineFactory {

    public static final String DECISION_TYPE = "driver-dispatch";

    private DispatchDecisionPipelineFactory() {}

    /**
     * Creates a RULES_ONLY pipeline (Constraints -> Business Rules -> Scoring -> Recommendation).
     */
    public static DecisionPipeline createRulesOnlyPipeline() {
        return LogistiX.pipelineBuilder(DECISION_TYPE)
                .name("Rules-Only Driver Dispatch Pipeline")
                .step(new DriverDispatchConstraintStep())
                .step(new DriverDispatchRuleStep())
                .step(new DriverDispatchScoringStep())
                .step(new DriverDispatchRecommendationStep())
                .build();
    }

    /**
     * Backward-compatible alias for createRulesOnlyPipeline().
     */
    public static DecisionPipeline createDeterministicPipeline() {
        return createRulesOnlyPipeline();
    }

    /**
     * Creates an AI_ASSISTED pipeline (Constraints -> Business Rules -> AI Advisor -> Recommendation).
     */
    public static DecisionPipeline createAiAssistedPipeline(AIProvider aiProvider) {
        return LogistiX.pipelineBuilder(DECISION_TYPE)
                .name("AI-Assisted Driver Dispatch Pipeline")
                .step(new DriverDispatchConstraintStep())
                .step(new DriverDispatchRuleStep())
                .step(new DriverDispatchAIStep(aiProvider))
                .step(new DriverDispatchRecommendationStep())
                .build();
    }

    /**
     * Creates a HYBRID pipeline (Constraints -> Business Rules -> Scoring -> AI Advisor -> Recommendation).
     */
    public static DecisionPipeline createHybridAiPipeline() {
        return createHybridAiPipeline(new DispatchAIAdvisor());
    }

    /**
     * Creates a HYBRID pipeline using a specific AIProvider SPI implementation.
     */
    public static DecisionPipeline createHybridAiPipeline(AIProvider aiProvider) {
        return LogistiX.pipelineBuilder(DECISION_TYPE)
                .name("Hybrid AI-Assisted Driver Dispatch Pipeline")
                .step(new DriverDispatchConstraintStep())
                .step(new DriverDispatchRuleStep())
                .step(new DriverDispatchScoringStep())
                .step(new DriverDispatchAIStep(aiProvider))
                .step(new DriverDispatchRecommendationStep())
                .build();
    }
}
