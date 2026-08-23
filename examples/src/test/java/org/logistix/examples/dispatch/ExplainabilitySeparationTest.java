package org.logistix.examples.dispatch;

import org.logistix.common.enums.PriorityLevel;
import org.logistix.common.model.Coordinates;
import org.logistix.domain.decision.DecisionContext;
import org.logistix.domain.decision.DecisionResult;
import org.logistix.domain.explanation.Explanation;
import org.logistix.domain.explanation.FeatureContribution;
import org.logistix.domain.fact.Fact;
import org.logistix.domain.fact.FactBag;
import org.logistix.dsl.LogistiX;
import org.logistix.engine.executor.DecisionExecutor;
import org.logistix.engine.pipeline.DecisionPipeline;
import org.logistix.examples.dispatch.ai.DispatchAIAdvisor;
import org.logistix.examples.dispatch.model.DispatchAssignment;
import org.logistix.examples.dispatch.model.DispatchCandidate;
import org.logistix.examples.dispatch.model.Driver;
import org.logistix.examples.dispatch.model.DriverTier;
import org.logistix.examples.dispatch.model.Shipment;
import org.logistix.examples.dispatch.pipeline.DispatchDecisionPipelineFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ExplainabilitySeparationTest {

    @Test
    @DisplayName("Explainability must clearly separate deterministic feature contributions from AI contextual insights")
    void testExplainabilitySeparation() {
        Instant now = Instant.now();

        Shipment shipment = Shipment.builder()
                .origin(Coordinates.of(37.7749, -122.4194))
                .destination(Coordinates.of(34.0522, -118.2437))
                .deliveryDeadline(now.plus(Duration.ofHours(12)))
                .priority(PriorityLevel.HIGH)
                .build();

        Driver driver = Driver.builder()
                .name("Alex 'Swift' Rivera")
                .tier(DriverTier.PLATINUM)
                .remainingHos(Duration.ofHours(10))
                .build();

        List<DispatchCandidate> candidates = List.of(
                DispatchCandidate.from(driver, shipment, now, 0.15, "CLEAR")
        );

        DecisionPipeline pipeline = DispatchDecisionPipelineFactory.createHybridAiPipeline(new DispatchAIAdvisor());
        DecisionExecutor executor = LogistiX.getContext().getExecutor();

        DecisionContext context = DecisionContext.of(
                DispatchDecisionPipelineFactory.DECISION_TYPE,
                FactBag.of(
                        Fact.of("candidates", candidates),
                        Fact.of("shipment", shipment)
                ),
                Map.of("weatherAdvisory", "RAIN_SHOWER"),
                Map.of("test", true)
        );

        DecisionResult<DispatchAssignment> result = executor.execute(pipeline, context);

        Explanation explanation = result.explanation();
        assertThat(explanation).isNotNull();

        // 1. Verify Deterministic Feature Contributions
        List<FeatureContribution> contributions = explanation.featureContributions();
        assertThat(contributions).isNotEmpty();
        assertThat(contributions.stream().map(FeatureContribution::featureName))
                .contains("Deadhead Proximity", "ETA SLA Margin", "Driver Rating & On-Time History", "Trip Cost Efficiency", "Business Rule Incentives");

        // 2. Verify AI Contextual Narrative is isolated in Key Factors
        List<String> keyFactors = explanation.keyFactors();
        assertThat(keyFactors.stream().anyMatch(k -> k.startsWith("AI Context"))).isTrue();

        // 3. Verify Distinction between Decision Confidence and AI Advisory Confidence in Metadata
        Map<String, Object> metadata = result.recommendation().metadata();
        assertThat(metadata).containsKeys("decisionConfidence", "aiEnrichmentStatus", "aiProvider", "aiAdvisoryConfidence");
        assertThat(metadata.get("aiEnrichmentStatus")).isEqualTo("SUCCESS");
        assertThat((Double) metadata.get("decisionConfidence")).isEqualTo(result.score().confidence());
        assertThat((Double) metadata.get("aiAdvisoryConfidence")).isEqualTo(0.92);
    }
}
