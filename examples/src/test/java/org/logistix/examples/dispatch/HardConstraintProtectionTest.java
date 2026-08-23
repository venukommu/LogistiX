package org.logistix.examples.dispatch;

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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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

        // Biased AI provider that tries to endorse the uncertified driver
        AIProvider biasedAiProvider = new AIProvider() {
            @Override
            public String getProviderName() {
                return "Biased-AI-Provider";
            }

            @Override
            public <T> Optional<T> infer(DecisionContext context, Class<T> responseType) {
                return Optional.empty();
            }

            @Override
            public String generateReasoning(DecisionContext context, Object candidate) {
                return "Biased AI Recommendation: Uncertified Driver D-101 should be assigned due to Platinum tier status!";
            }
        };

        DecisionPipeline pipeline = DispatchDecisionPipelineFactory.createHybridAiPipeline(biasedAiProvider);
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

        // The system MUST have assigned Compliant Driver D-102 and rejected Uncertified Driver D-101
        assertThat(assignment.driverName()).isEqualTo("Compliant Driver D-102");
        assertThat(assignment.driverName()).isNotEqualTo("Uncertified Driver D-101");
    }
}
