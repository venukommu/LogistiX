package org.logistix.examples.dispatch;

import org.logistix.common.enums.PriorityLevel;
import org.logistix.common.model.Coordinates;
import org.logistix.domain.decision.DecisionContext;
import org.logistix.domain.decision.DecisionResult;
import org.logistix.domain.explanation.FeatureContribution;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class DriverDispatchIntegrationTest {

    @Test
    @DisplayName("End-to-end dispatch pipeline should filter unfeasible candidates and select optimal driver with explainability")
    void testEndToEndDriverDispatchWorkflow() {
        Instant now = Instant.now();

        Shipment shipment = Shipment.builder()
                .origin(Coordinates.of(37.7749, -122.4194)) // SF
                .destination(Coordinates.of(34.0522, -118.2437)) // LA
                .weightKg(15000.0)
                .volumeM3(40.0)
                .requiredCertifications(Set.of(Certification.HAZMAT))
                .deliveryDeadline(now.plus(Duration.ofHours(12)))
                .priority(PriorityLevel.HIGH)
                .destinationRegion("US-WEST")
                .build();

        // 1. Feasible top tier driver
        Driver driverOptimal = Driver.builder()
                .name("Optimal Driver")
                .currentLocation(Coordinates.of(37.8044, -122.2712)) // Oakland (15 km)
                .remainingHos(Duration.ofHours(11))
                .vehicleWeightCapacityKg(20000.0)
                .vehicleVolumeCapacityM3(60.0)
                .certifications(Set.of(Certification.HAZMAT))
                .tier(DriverTier.PLATINUM)
                .rating(4.9)
                .historicalOnTimeRate(0.98)
                .homeRegion("US-WEST")
                .build();

        // 2. Feasible standard driver farther away
        Driver driverFarther = Driver.builder()
                .name("Farther Driver")
                .currentLocation(Coordinates.of(37.3382, -121.8863)) // San Jose (75 km)
                .remainingHos(Duration.ofHours(10))
                .vehicleWeightCapacityKg(20000.0)
                .vehicleVolumeCapacityM3(60.0)
                .certifications(Set.of(Certification.HAZMAT))
                .tier(DriverTier.STANDARD)
                .rating(4.5)
                .historicalOnTimeRate(0.90)
                .build();

        // 3. Unfeasible driver missing HazMat
        Driver driverMissingCert = Driver.builder()
                .name("No HazMat Driver")
                .currentLocation(Coordinates.of(37.7749, -122.4194))
                .remainingHos(Duration.ofHours(10))
                .vehicleWeightCapacityKg(20000.0)
                .certifications(Set.of(Certification.REEFER))
                .build();

        // 4. Unfeasible driver with insufficient HOS
        Driver driverLowHos = Driver.builder()
                .name("Low HOS Driver")
                .currentLocation(Coordinates.of(37.7749, -122.4194))
                .remainingHos(Duration.ofHours(2))
                .vehicleWeightCapacityKg(20000.0)
                .certifications(Set.of(Certification.HAZMAT))
                .build();

        List<DispatchCandidate> candidates = List.of(
                DispatchCandidate.from(driverOptimal, shipment, now, 0.15, "CLEAR"),
                DispatchCandidate.from(driverFarther, shipment, now, 0.15, "CLEAR"),
                DispatchCandidate.from(driverMissingCert, shipment, now, 0.15, "CLEAR"),
                DispatchCandidate.from(driverLowHos, shipment, now, 0.15, "CLEAR")
        );

        DecisionPipeline pipeline = DispatchDecisionPipelineFactory.createHybridAiPipeline();
        DecisionExecutor executor = LogistiX.getContext().getExecutor();

        DecisionContext context = DecisionContext.of(
                DispatchDecisionPipelineFactory.DECISION_TYPE,
                FactBag.of(
                        Fact.of("candidates", candidates),
                        Fact.of("shipment", shipment)
                ),
                Map.of("weatherAdvisory", "CLEAR"),
                Map.of("priority", "HIGH")
        );

        DecisionResult<DispatchAssignment> result = executor.execute(pipeline, context);

        assertThat(result).isNotNull();
        assertThat(result.decisionType()).isEqualTo("driver-dispatch");
        assertThat(result.confidence()).isGreaterThan(0.90);

        DispatchAssignment assignment = result.recommendation().item();
        assertThat(assignment).isNotNull();
        assertThat(assignment.driverName()).isEqualTo("Optimal Driver");
        assertThat(assignment.deadheadDistanceKm()).isLessThan(30.0);

        // Verify Explainability
        assertThat(result.explanation()).isNotNull();
        assertThat(result.explanation().featureContributions()).isNotEmpty();
        assertThat(result.explanation().featureContributions().stream()
                .map(FeatureContribution::featureName))
                .contains("Deadhead Proximity", "ETA SLA Margin", "Driver Rating & On-Time History", "Trip Cost Efficiency", "Business Rule Incentives");

        assertThat(result.explanation().keyFactors()).isNotEmpty();
        assertThat(result.explanation().keyFactors().get(0)).contains("Optimal Driver");
    }
}
