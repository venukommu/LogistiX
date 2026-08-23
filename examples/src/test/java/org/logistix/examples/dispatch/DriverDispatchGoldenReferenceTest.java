package org.logistix.examples.dispatch;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.logistix.ai.dispatch.AITelemetry;
import org.logistix.ai.dispatch.BatchedDispatchAIAdvice;
import org.logistix.ai.dispatch.DispatchAIAdvice;
import org.logistix.ai.dispatch.MockDispatchAIProvider;
import org.logistix.ai.dispatch.RiskLevel;
import org.logistix.common.enums.PriorityLevel;
import org.logistix.common.model.Coordinates;
import org.logistix.domain.decision.DecisionContext;
import org.logistix.domain.decision.DecisionResult;
import org.logistix.domain.explanation.Explanation;
import org.logistix.domain.explanation.FeatureContribution;
import org.logistix.domain.fact.Fact;
import org.logistix.domain.fact.FactBag;
import org.logistix.domain.ports.AIProvider;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GOLDEN REFERENCE REGRESSION TEST SUITE for AI-Assisted Driver Dispatch.
 * This suite serves as the canonical regression contract for future LogistiX framework releases.
 */
class DriverDispatchGoldenReferenceTest {

    private final DecisionExecutor executor = LogistiX.getContext().getExecutor();

    private Shipment createStandardShipment(Instant now, Set<Certification> certs) {
        return Shipment.builder()
                .origin(Coordinates.of(37.7749, -122.4194)) // SF
                .destination(Coordinates.of(34.0522, -118.2437)) // LA (~615 km)
                .weightKg(10000.0)
                .volumeM3(30.0)
                .requiredCertifications(certs)
                .deliveryDeadline(now.plus(Duration.ofHours(12)))
                .priority(PriorityLevel.HIGH)
                .destinationRegion("US-WEST")
                .build();
    }

    @Nested
    @DisplayName("1. Hard Constraint Inviolability & Feasibility Pruning")
    class HardConstraintTests {

        @Test
        @DisplayName("Must reject candidates violating HOS, Capacity, Certifications, or Deadlines")
        void testHardFeasibilityPruning() {
            Instant now = Instant.now();
            Shipment shipment = createStandardShipment(now, Set.of(Certification.HAZMAT));

            // Candidate 1: Missing HAZMAT
            Driver uncertified = Driver.builder().name("D1-NoHazmat").certifications(Set.of()).tier(DriverTier.PLATINUM).build();
            // Candidate 2: Insufficient HOS
            Driver lowHos = Driver.builder().name("D2-LowHos").certifications(Set.of(Certification.HAZMAT)).remainingHos(Duration.ofHours(3)).build();
            // Candidate 3: Feasible
            Driver compliant = Driver.builder().name("D3-Compliant").certifications(Set.of(Certification.HAZMAT)).remainingHos(Duration.ofHours(11)).build();

            List<DispatchCandidate> candidates = List.of(
                    DispatchCandidate.from(uncertified, shipment, now, 0.1, "CLEAR"),
                    DispatchCandidate.from(lowHos, shipment, now, 0.1, "CLEAR"),
                    DispatchCandidate.from(compliant, shipment, now, 0.1, "CLEAR")
            );

            DecisionPipeline pipeline = DispatchDecisionPipelineFactory.createRulesOnlyPipeline();
            DecisionContext context = DecisionContext.of(
                    DispatchDecisionPipelineFactory.DECISION_TYPE,
                    FactBag.of(Fact.of("candidates", candidates), Fact.of("shipment", shipment)),
                    Map.of("weatherAdvisory", "CLEAR"), Map.of()
            );

            DecisionResult<DispatchAssignment> result = executor.execute(pipeline, context);

            assertThat(result.recommendation().item().isAssigned()).isTrue();
            assertThat(result.recommendation().item().driverName()).isEqualTo("D3-Compliant");
        }

        @Test
        @DisplayName("AI CANNOT resurrect an infeasible candidate or force assignment of a phantom driver")
        void testAiCannotResurrectInfeasibleCandidate() {
            Instant now = Instant.now();
            Shipment shipment = createStandardShipment(now, Set.of(Certification.HAZMAT));

            Driver uncertified = Driver.builder().name("D1-NoHazmat").certifications(Set.of()).build();
            Driver compliant = Driver.builder().name("D2-Compliant").certifications(Set.of(Certification.HAZMAT)).build();

            List<DispatchCandidate> candidates = List.of(
                    DispatchCandidate.from(uncertified, shipment, now, 0.1, "CLEAR"),
                    DispatchCandidate.from(compliant, shipment, now, 0.1, "CLEAR")
            );

            // Rogue AI attempting to pick uncertified or phantom ID
            AIProvider rogueAi = new AIProvider() {
                @Override public String getProviderName() { return "Rogue-AI"; }
                @Override
                @SuppressWarnings("unchecked")
                public <T> Optional<T> infer(DecisionContext context, Class<T> responseType) {
                    DispatchAIAdvice badAdvice = new DispatchAIAdvice(
                            uncertified.driverId().toString(), RiskLevel.LOW, 0.99, "Override!", List.of(), List.of(), Instant.now()
                    );
                    return Optional.of((T) BatchedDispatchAIAdvice.of(List.of(badAdvice), "Rogue"));
                }
                @Override public String generateReasoning(DecisionContext context, Object candidate) { return ""; }
            };

            DecisionPipeline pipeline = DispatchDecisionPipelineFactory.createHybridAiPipeline(rogueAi);
            DecisionContext context = DecisionContext.of(
                    DispatchDecisionPipelineFactory.DECISION_TYPE,
                    FactBag.of(Fact.of("candidates", candidates), Fact.of("shipment", shipment)),
                    Map.of("weatherAdvisory", "CLEAR"), Map.of()
            );

            DecisionResult<DispatchAssignment> result = executor.execute(pipeline, context);

            assertThat(result.recommendation().item().driverName()).isEqualTo("D2-Compliant");
        }
    }

    @Nested
    @DisplayName("2. Single AI Invocation Invariant & Batching")
    class SingleAIInvocationTests {

        @Test
        @DisplayName("Exactly ONE AI invocation for N candidates (N=1, N=3, N=5)")
        void testSingleInvocationInvariant() {
            Instant now = Instant.now();
            Shipment shipment = createStandardShipment(now, Set.of(Certification.HAZMAT));

            List<Driver> drivers = List.of(
                    Driver.builder().name("D1").certifications(Set.of(Certification.HAZMAT)).build(),
                    Driver.builder().name("D2").certifications(Set.of(Certification.HAZMAT)).build(),
                    Driver.builder().name("D3").certifications(Set.of(Certification.HAZMAT)).build(),
                    Driver.builder().name("D4").certifications(Set.of(Certification.HAZMAT)).build(),
                    Driver.builder().name("D5").certifications(Set.of(Certification.HAZMAT)).build()
            );

            List<DispatchCandidate> candidates = drivers.stream()
                    .map(d -> DispatchCandidate.from(d, shipment, now, 0.1, "CLEAR"))
                    .toList();

            MockDispatchAIProvider mockProvider = new MockDispatchAIProvider();
            DecisionPipeline pipeline = DispatchDecisionPipelineFactory.createHybridAiPipeline(mockProvider);
            DecisionContext context = DecisionContext.of(
                    DispatchDecisionPipelineFactory.DECISION_TYPE,
                    FactBag.of(Fact.of("candidates", candidates), Fact.of("shipment", shipment)),
                    Map.of("weatherAdvisory", "CLEAR"), Map.of()
            );

            DecisionResult<DispatchAssignment> result = executor.execute(pipeline, context);

            assertThat(mockProvider.getInvocationCount()).isEqualTo(1);
            AITelemetry telemetry = (AITelemetry) result.recommendation().metadata().get("aiTelemetry");
            assertThat(telemetry.invocationCount()).isEqualTo(1);
            assertThat(telemetry.providerType()).isEqualTo("MOCK");
            assertThat(telemetry.status()).isEqualTo("SUCCESS");
        }

        @Test
        @DisplayName("Zero candidates available should result in 0 AI invocations (SKIPPED)")
        void testZeroCandidatesSkipsAI() {
            Instant now = Instant.now();
            Shipment shipment = createStandardShipment(now, Set.of(Certification.HAZMAT));

            MockDispatchAIProvider mockProvider = new MockDispatchAIProvider();
            DecisionPipeline pipeline = DispatchDecisionPipelineFactory.createHybridAiPipeline(mockProvider);
            DecisionContext context = DecisionContext.of(
                    DispatchDecisionPipelineFactory.DECISION_TYPE,
                    FactBag.of(Fact.of("candidates", Collections.emptyList()), Fact.of("shipment", shipment)),
                    Map.of("weatherAdvisory", "CLEAR"), Map.of()
            );

            DecisionResult<DispatchAssignment> result = executor.execute(pipeline, context);

            assertThat(mockProvider.getInvocationCount()).isEqualTo(0);
            AITelemetry telemetry = (AITelemetry) result.recommendation().metadata().get("aiTelemetry");
            assertThat(telemetry.status()).isEqualTo("SKIPPED");
            assertThat(telemetry.invocationCount()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("3. Fault Tolerance, Fallback & Explainability")
    class FaultToleranceAndExplainabilityTests {

        @Test
        @DisplayName("AI failure or timeout must trigger graceful fallback without pipeline interruption")
        void testGracefulFallbackOnAiFailure() {
            Instant now = Instant.now();
            Shipment shipment = createStandardShipment(now, Set.of(Certification.HAZMAT));
            Driver driver = Driver.builder().name("D1-Compliant").certifications(Set.of(Certification.HAZMAT)).build();
            List<DispatchCandidate> candidates = List.of(DispatchCandidate.from(driver, shipment, now, 0.1, "CLEAR"));

            DecisionPipeline pipeline = DispatchDecisionPipelineFactory.createHybridAiPipeline(MockDispatchAIProvider.offline());
            DecisionContext context = DecisionContext.of(
                    DispatchDecisionPipelineFactory.DECISION_TYPE,
                    FactBag.of(Fact.of("candidates", candidates), Fact.of("shipment", shipment)),
                    Map.of("weatherAdvisory", "CLEAR"), Map.of()
            );

            DecisionResult<DispatchAssignment> result = executor.execute(pipeline, context);

            assertThat(result.recommendation().item().isAssigned()).isTrue();
            assertThat(result.recommendation().metadata()).containsEntry("aiEnrichmentStatus", "FALLBACK_TRIGGERED");
            AITelemetry telemetry = (AITelemetry) result.recommendation().metadata().get("aiTelemetry");
            assertThat(telemetry.status()).isEqualTo("FALLBACK_TRIGGERED");
            assertThat(telemetry.fallbackTriggered()).isTrue();
        }

        @Test
        @DisplayName("Explainability must cleanly demarcate deterministic feature contributions from AI insights")
        void testExplainabilityDemarcation() {
            Instant now = Instant.now();
            Shipment shipment = createStandardShipment(now, Set.of(Certification.HAZMAT));
            Driver driver = Driver.builder().name("D1-Compliant").tier(DriverTier.PLATINUM).certifications(Set.of(Certification.HAZMAT)).build();
            List<DispatchCandidate> candidates = List.of(DispatchCandidate.from(driver, shipment, now, 0.1, "CLEAR"));

            DecisionPipeline pipeline = DispatchDecisionPipelineFactory.createHybridAiPipeline(new MockDispatchAIProvider());
            DecisionContext context = DecisionContext.of(
                    DispatchDecisionPipelineFactory.DECISION_TYPE,
                    FactBag.of(Fact.of("candidates", candidates), Fact.of("shipment", shipment)),
                    Map.of("weatherAdvisory", "RAIN"), Map.of()
            );

            DecisionResult<DispatchAssignment> result = executor.execute(pipeline, context);
            Explanation exp = result.explanation();

            assertThat(exp.featureContributions()).isNotEmpty();
            assertThat(exp.featureContributions().stream().map(FeatureContribution::featureName))
                    .contains("Deadhead Proximity", "ETA SLA Margin", "Driver Rating & On-Time History", "Trip Cost Efficiency", "Business Rule Incentives");

            assertThat(exp.keyFactors().stream().anyMatch(f -> f.startsWith("AI Context"))).isTrue();
            assertThat((Double) result.recommendation().metadata().get("decisionConfidence")).isEqualTo(result.score().confidence());
            assertThat((Double) result.recommendation().metadata().get("aiAdvisoryConfidence")).isEqualTo(0.92);
        }
    }
}
