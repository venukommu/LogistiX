package org.logistix.examples;

import org.logistix.domain.decision.DecisionContext;
import org.logistix.domain.decision.DecisionResult;
import org.logistix.domain.explanation.Explanation;
import org.logistix.domain.recommendation.Recommendation;
import org.logistix.domain.score.Score;
import org.logistix.dsl.LogistiX;
import org.logistix.engine.pipeline.DecisionPipeline;
import org.logistix.engine.steps.ConstraintStep;
import org.logistix.engine.steps.RecommendationStep;
import org.logistix.engine.steps.RuleStep;
import org.logistix.engine.steps.ScoringStep;
import org.logistix.engine.steps.StepMetadata;
import org.logistix.engine.steps.StepResult;

import java.time.Duration;

/**
 * <h3>Simple Decision Pipeline Example</h3>
 * Illustrates assembling an immutable multi-step decision pipeline using the fluent DSL.
 */
public class SimpleDecisionPipeline {

    public static void main(String[] args) {
        // 1. Build an immutable decision pipeline
        DecisionPipeline pipeline = LogistiX.pipeline("carrier-recommendation")
                .name("Standard-Carrier-Routing-Pipeline")
                .version("1.0.0")
                .step(new SampleConstraintStep())
                .step(new SampleRuleStep())
                .step(new SampleScoringStep())
                .step(new SampleRecommendationStep())
                .build();

        // 2. Register pipeline in the runtime
        LogistiX.getContext().getDecisionRegistry().register(pipeline);

        // 3. Execute decision
        DecisionResult<String> result = LogistiX.<String>decision("carrier-recommendation")
                .fact("lane", "LAX -> JFK")
                .fact("requiredTransitHours", 48)
                .execute();

        System.out.println("Pipeline Result: " + result.recommendation().item());
    }

    static class SampleConstraintStep implements ConstraintStep {
        @Override
        public StepMetadata getMetadata() {
            return StepMetadata.of("constraint-step-1", "Check Carrier Availability", 1);
        }

        @Override
        public StepResult execute(DecisionContext context) {
            return StepResult.success(context, Duration.ofMillis(2), "All carriers verified eligible");
        }
    }

    static class SampleRuleStep implements RuleStep {
        @Override
        public StepMetadata getMetadata() {
            return StepMetadata.of("rule-step-1", "Apply Tier-1 Service Level Agreements", 2);
        }

        @Override
        public StepResult execute(DecisionContext context) {
            return StepResult.success(context, Duration.ofMillis(3), "SLA rules satisfied");
        }
    }

    static class SampleScoringStep implements ScoringStep {
        @Override
        public StepMetadata getMetadata() {
            return StepMetadata.of("scoring-step-1", "Compute Multi-Criteria Score", 3);
        }

        @Override
        public StepResult execute(DecisionContext context) {
            return StepResult.success(context, Duration.ofMillis(5), "Weighted scoring calculated");
        }
    }

    static class SampleRecommendationStep implements RecommendationStep {
        @Override
        public StepMetadata getMetadata() {
            return StepMetadata.of("rec-step-1", "Synthesize Recommendation", 4);
        }

        @Override
        public StepResult execute(DecisionContext context) {
            Recommendation<String> recommendation = Recommendation.of(
                    "Apex Freight Logistics",
                    1,
                    Score.of(0.94, 0.98),
                    "Optimal rate-to-reliability ratio on LAX -> JFK lane"
            );
            Explanation explanation = Explanation.simple("Apex Freight has 99.2% on-time delivery across 1,200 runs.", 0.98);

            DecisionContext updated = LogistiX.context("carrier-recommendation")
                    .facts(context.facts())
                    .fact("recommendation", recommendation)
                    .fact("explanation", explanation)
                    .build();

            return StepResult.success(updated, Duration.ofMillis(2), "Top recommendation synthesized");
        }
    }
}
