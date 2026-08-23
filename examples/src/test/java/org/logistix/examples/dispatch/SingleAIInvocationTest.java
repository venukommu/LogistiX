package org.logistix.examples.dispatch;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.logistix.ai.dispatch.AITelemetry;
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
import org.logistix.examples.dispatch.model.Certification;
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
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SingleAIInvocationTest {

    @Test
    @DisplayName("Phase 2 Requirement: Exactly ONE batched AI invocation per dispatch decision")
    void testOneDecisionProducesOneAIInvocation() {
        Instant now = Instant.now();

        Shipment shipment = Shipment.builder()
                .origin(Coordinates.of(37.7749, -122.4194))
                .destination(Coordinates.of(34.0522, -118.2437))
                .weightKg(10000.0)
                .requiredCertifications(Set.of(Certification.HAZMAT))
                .deliveryDeadline(now.plus(Duration.ofHours(12)))
                .priority(PriorityLevel.HIGH)
                .build();

        // 5 candidates
        List<Driver> drivers = List.of(
                Driver.builder().name("Driver 1").certifications(Set.of(Certification.HAZMAT)).tier(DriverTier.PLATINUM).build(),
                Driver.builder().name("Driver 2").certifications(Set.of(Certification.HAZMAT)).tier(DriverTier.GOLD).build(),
                Driver.builder().name("Driver 3").certifications(Set.of(Certification.HAZMAT)).tier(DriverTier.SILVER).build(),
                Driver.builder().name("Driver 4").certifications(Set.of(Certification.HAZMAT)).tier(DriverTier.STANDARD).build(),
                Driver.builder().name("Driver 5").certifications(Set.of(Certification.HAZMAT)).tier(DriverTier.STANDARD).build()
        );

        List<DispatchCandidate> candidates = drivers.stream()
                .map(d -> DispatchCandidate.from(d, shipment, now, 0.1, "CLEAR"))
                .toList();

        MockDispatchAIProvider mockProvider = new MockDispatchAIProvider();
        DecisionPipeline pipeline = DispatchDecisionPipelineFactory.createHybridAiPipeline(mockProvider);
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
        assertThat(result.recommendation().item().isAssigned()).isTrue();

        // Verify invocation count on mock provider is EXACTLY 1
        assertThat(mockProvider.getInvocationCount()).isEqualTo(1);

        // Verify typed AI telemetry in recommendation metadata
        Map<String, Object> meta = result.recommendation().metadata();
        assertThat(meta).containsKey("aiTelemetry");
        AITelemetry telemetry = (AITelemetry) meta.get("aiTelemetry");
        assertThat(telemetry.invocationCount()).isEqualTo(1);
        assertThat(telemetry.status()).isEqualTo("SUCCESS");
        assertThat(telemetry.fallbackTriggered()).isFalse();
    }
}
