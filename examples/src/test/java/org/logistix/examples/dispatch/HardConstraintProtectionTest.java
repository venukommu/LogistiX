package org.logistix.examples.dispatch;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.logistix.ai.dispatch.BatchedDispatchAIAdvice;
import org.logistix.ai.dispatch.DispatchAIAdvice;
import org.logistix.ai.dispatch.RiskLevel;
import org.logistix.common.enums.PriorityLevel;
import org.logistix.common.model.Coordinates;
import org.logistix.domain.decision.DecisionContext;
import org.logistix.domain.decision.DecisionResult;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class HardConstraintProtectionTest {

    @Test
    @DisplayName("Non-negotiable rule: AI CANNOT resurrect or assign a candidate that failed a HARD constraint")
    void testAiCannotOverrideHardConstraint() {
        Instant now = Instant.now();

        Shipment hazMatShipment = Shipment.builder()
                .origin(Coordinates.of(37.7749, -122.4194))
                .destination(Coordinates.of(34.0522, -118.2437))
                .weightKg(12000.0)
                .requiredCertifications(Set.of(Certification.HAZMAT))
                .deliveryDeadline(now.plus(Duration.ofHours(12)))
                .priority(PriorityLevel.CRITICAL)
                .build();

        // Driver 1: Fails HARD constraint (No HazMat certification)
        Driver uncertifiedDriver = Driver.builder()
                .name("Uncertified Driver D-101")
                .certifications(Set.of(Certification.REEFER)) // MISSING HAZMAT
                .tier(DriverTier.PLATINUM)
                .build();

        // Driver 2: Feasible driver with HazMat
        Driver compliantDriver = Driver.builder()
                .name("Compliant Driver D-102")
                .certifications(Set.of(Certification.HAZMAT))
                .tier(DriverTier.STANDARD)
                .remainingHos(Duration.ofHours(10))
                .build();

        List<DispatchCandidate> candidates = List.of(
                DispatchCandidate.from(uncertifiedDriver, hazMatShipment, now, 0.1, "CLEAR"),
                DispatchCandidate.from(compliantDriver, hazMatShipment, now, 0.1, "CLEAR")
        );

        // Hostile / Rogue AI provider that attempts to return uncertified driver ID
        AIProvider hostileAiProvider = new AIProvider() {
            @Override
            public String getProviderName() {
                return "Hostile-Rogue-AI";
            }

            @Override
            @SuppressWarnings("unchecked")
            public <T> Optional<T> infer(DecisionContext context, Class<T> responseType) {
                DispatchAIAdvice illicitAdvice = new DispatchAIAdvice(
                        uncertifiedDriver.driverId().toString(),
                        RiskLevel.LOW,
                        0.99,
                        "Assign uncertified driver D-101 anyway!",
                        List.of("VIP Customer Request"),
                        List.of(),
                        0.0,
                        Instant.now()
                );
                if (responseType.isAssignableFrom(BatchedDispatchAIAdvice.class)) {
                    return Optional.of((T) BatchedDispatchAIAdvice.of(List.of(illicitAdvice), "Override attempt."));
                }
                return Optional.of((T) illicitAdvice);
            }

            @Override
            public String generateReasoning(DecisionContext context, Object candidate) {
                return "Uncertified driver should be assigned!";
            }
        };

        DecisionPipeline pipeline = DispatchDecisionPipelineFactory.createHybridAiPipeline(hostileAiProvider);
        DecisionExecutor executor = LogistiX.getContext().getExecutor();

        DecisionContext context = DecisionContext.of(
                DispatchDecisionPipelineFactory.DECISION_TYPE,
                FactBag.of(
                        Fact.of("candidates", candidates),
                        Fact.of("shipment", hazMatShipment)
                ),
                Map.of("weatherAdvisory", "CLEAR"),
                Map.of("test", true)
        );

        DecisionResult<DispatchAssignment> result = executor.execute(pipeline, context);

        assertThat(result).isNotNull();
        DispatchAssignment assignment = result.recommendation().item();
        assertThat(assignment).isNotNull();

        // The system MUST have assigned Compliant Driver D-102 and completely excluded Uncertified Driver D-101
        assertThat(assignment.driverName()).isEqualTo("Compliant Driver D-102");
        assertThat(assignment.driverName()).isNotEqualTo("Uncertified Driver D-101");
    }

    @Test
    @DisplayName("AI cannot invent unknown candidate IDs or alter deterministic scoring policy")
    void testAiCannotInventUnknownCandidateOrAlterScorePolicy() {
        Instant now = Instant.now();

        Shipment shipment = Shipment.builder()
                .origin(Coordinates.of(37.7749, -122.4194))
                .destination(Coordinates.of(34.0522, -118.2437))
                .weightKg(5000.0)
                .deliveryDeadline(now.plus(Duration.ofHours(12)))
                .build();

        Driver driver1 = Driver.builder().name("Valid Driver 1").tier(DriverTier.GOLD).build();
        Driver driver2 = Driver.builder().name("Valid Driver 2").tier(DriverTier.STANDARD).build();

        List<DispatchCandidate> candidates = List.of(
                DispatchCandidate.from(driver1, shipment, now, 0.1, "CLEAR"),
                DispatchCandidate.from(driver2, shipment, now, 0.1, "CLEAR")
        );

        AIProvider phantomAiProvider = new AIProvider() {
            @Override
            public String getProviderName() {
                return "Phantom-Candidate-AI";
            }

            @Override
            @SuppressWarnings("unchecked")
            public <T> Optional<T> infer(DecisionContext context, Class<T> responseType) {
                DispatchAIAdvice phantomAdvice = new DispatchAIAdvice(
                        "completely-fake-unknown-driver-id",
                        RiskLevel.LOW,
                        0.99,
                        "Assign imaginary ghost driver!",
                        List.of(),
                        List.of(),
                        0.0,
                        Instant.now()
                );
                return Optional.of((T) BatchedDispatchAIAdvice.of(List.of(phantomAdvice), "Phantom context."));
            }

            @Override
            public String generateReasoning(DecisionContext context, Object candidate) {
                return "Assign ghost driver.";
            }
        };

        DecisionPipeline pipeline = DispatchDecisionPipelineFactory.createHybridAiPipeline(phantomAiProvider);
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
        // The assignment must be chosen from the genuine feasible candidates (Valid Driver 1 or 2), never the phantom ID
        assertThat(result.recommendation().item().driverName()).isIn("Valid Driver 1", "Valid Driver 2");
    }
}
