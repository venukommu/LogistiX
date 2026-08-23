package org.logistix.examples.dispatch;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.logistix.ai.dispatch.MockDispatchAIProvider;
import org.logistix.common.enums.PriorityLevel;
import org.logistix.common.model.Coordinates;
import org.logistix.domain.decision.DecisionContext;
import org.logistix.domain.decision.DecisionResult;
import org.logistix.domain.fact.Fact;
import org.logistix.domain.fact.FactBag;
import org.logistix.dsl.LogistiX;
import org.logistix.engine.executor.DecisionExecutor;
import org.logistix.engine.pipeline.DecisionPipeline;
import org.logistix.examples.dispatch.model.DispatchAssignment;
import org.logistix.examples.dispatch.model.DispatchCandidate;
import org.logistix.examples.dispatch.model.Driver;
import org.logistix.examples.dispatch.model.DriverTier;
import org.logistix.examples.dispatch.model.Shipment;
import org.logistix.examples.dispatch.pipeline.DispatchDecisionPipelineFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AIFallbackTest {

    @Test
    @DisplayName("When AI provider fails or is offline, pipeline must degrade gracefully and produce valid assignment")
    void testAiDegradesGracefullyToDeterministicRules() {
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

        // Offline / faulty provider
        DecisionPipeline pipeline = DispatchDecisionPipelineFactory.createHybridAiPipeline(
                MockDispatchAIProvider.offline()
        );
        DecisionExecutor executor = LogistiX.getContext().getExecutor();

        DecisionContext context = DecisionContext.of(
                DispatchDecisionPipelineFactory.DECISION_TYPE,
                FactBag.of(
                        Fact.of("candidates", candidates),
                        Fact.of("shipment", shipment)
                ),
                Map.of("weatherAdvisory", "CLEAR"),
                Map.of("test", true)
        );

        DecisionResult<DispatchAssignment> result = executor.execute(pipeline, context);

        assertThat(result).isNotNull();
        assertThat(result.recommendation().item()).isNotNull();
        assertThat(result.recommendation().item().driverName()).isEqualTo("Alex 'Swift' Rivera");
        assertThat(result.recommendation().metadata()).containsEntry("aiEnrichmentStatus", "FALLBACK_TRIGGERED");
    }
}
