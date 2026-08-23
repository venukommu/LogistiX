package org.logistix.examples.dispatch;

import org.logistix.common.model.Coordinates;
import org.logistix.domain.constraint.ConstraintSeverity;
import org.logistix.domain.constraint.ConstraintViolation;
import org.logistix.domain.decision.DecisionContext;
import org.logistix.examples.dispatch.constraints.DeliveryDeadlineConstraint;
import org.logistix.examples.dispatch.constraints.DriverCertificationConstraint;
import org.logistix.examples.dispatch.constraints.DriverDispatchConstraintStep;
import org.logistix.examples.dispatch.constraints.HoursOfServiceConstraint;
import org.logistix.examples.dispatch.constraints.VehicleCapacityConstraint;
import org.logistix.examples.dispatch.model.Certification;
import org.logistix.examples.dispatch.model.DispatchCandidate;
import org.logistix.examples.dispatch.model.Driver;
import org.logistix.examples.dispatch.model.Shipment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ConstraintEngineTest {

    private DecisionContext context;
    private Shipment standardShipment;
    private Instant now;

    @BeforeEach
    void setUp() {
        context = DecisionContext.of("driver-dispatch");
        now = Instant.now();

        standardShipment = Shipment.builder()
                .origin(Coordinates.of(37.7749, -122.4194)) // SF
                .destination(Coordinates.of(34.0522, -118.2437)) // LA (~615 km, ~8.5 hours)
                .weightKg(10000.0)
                .volumeM3(30.0)
                .requiredCertifications(Set.of(Certification.HAZMAT))
                .deliveryDeadline(now.plus(Duration.ofHours(12)))
                .build();
    }

    @Test
    @DisplayName("HOS Constraint should pass when driver has sufficient remaining hours")
    void testHosConstraintPass() {
        Driver driver = Driver.builder()
                .remainingHos(Duration.ofHours(10))
                .build();

        DispatchCandidate candidate = DispatchCandidate.from(driver, standardShipment, now, 0.1, "CLEAR");
        HoursOfServiceConstraint constraint = new HoursOfServiceConstraint();

        Optional<ConstraintViolation> violation = constraint.evaluate(candidate, context);
        assertThat(violation).isEmpty();
    }

    @Test
    @DisplayName("HOS Constraint should fail when driver remaining hours are less than transit time")
    void testHosConstraintFail() {
        Driver driver = Driver.builder()
                .remainingHos(Duration.ofHours(4)) // Route takes ~8.5h
                .build();

        DispatchCandidate candidate = DispatchCandidate.from(driver, standardShipment, now, 0.1, "CLEAR");
        HoursOfServiceConstraint constraint = new HoursOfServiceConstraint();

        Optional<ConstraintViolation> violation = constraint.evaluate(candidate, context);
        assertThat(violation).isPresent();
        assertThat(violation.get().severity()).isEqualTo(ConstraintSeverity.HARD);
        assertThat(violation.get().constraintId()).isEqualTo(HoursOfServiceConstraint.ID);
    }

    @Test
    @DisplayName("Vehicle Capacity Constraint should reject payload exceeding maximum weight or volume")
    void testVehicleCapacityConstraint() {
        Driver driverLowWeight = Driver.builder()
                .vehicleWeightCapacityKg(8000.0) // Shipment requires 10,000 kg
                .vehicleVolumeCapacityM3(50.0)
                .build();

        Driver driverLowVol = Driver.builder()
                .vehicleWeightCapacityKg(20000.0)
                .vehicleVolumeCapacityM3(20.0) // Shipment requires 30 m3
                .build();

        VehicleCapacityConstraint constraint = new VehicleCapacityConstraint();

        DispatchCandidate cand1 = DispatchCandidate.from(driverLowWeight, standardShipment, now, 0.1, "CLEAR");
        DispatchCandidate cand2 = DispatchCandidate.from(driverLowVol, standardShipment, now, 0.1, "CLEAR");

        assertThat(constraint.evaluate(cand1, context)).isPresent();
        assertThat(constraint.evaluate(cand2, context)).isPresent();
    }

    @Test
    @DisplayName("Certification Constraint should pass only when all required endorsements are held")
    void testCertificationConstraint() {
        Driver driverWithHazMat = Driver.builder()
                .certifications(Set.of(Certification.HAZMAT, Certification.TWIC))
                .build();

        Driver driverWithoutHazMat = Driver.builder()
                .certifications(Set.of(Certification.REEFER))
                .build();

        DriverCertificationConstraint constraint = new DriverCertificationConstraint();

        DispatchCandidate candPass = DispatchCandidate.from(driverWithHazMat, standardShipment, now, 0.1, "CLEAR");
        DispatchCandidate candFail = DispatchCandidate.from(driverWithoutHazMat, standardShipment, now, 0.1, "CLEAR");

        assertThat(constraint.evaluate(candPass, context)).isEmpty();
        assertThat(constraint.evaluate(candFail, context)).isPresent();
        assertThat(constraint.evaluate(candFail, context).get().message()).contains("missing required certifications");
    }

    @Test
    @DisplayName("Delivery Deadline Constraint should reject drivers whose arrival exceeds SLA window")
    void testDeliveryDeadlineConstraint() {
        Shipment tightDeadlineShipment = Shipment.builder()
                .origin(Coordinates.of(37.7749, -122.4194))
                .destination(Coordinates.of(34.0522, -118.2437))
                .deliveryDeadline(now.plus(Duration.ofHours(3))) // 3 hours deadline for an 8.5 hour drive!
                .build();

        Driver driver = Driver.builder().build();
        DispatchCandidate candidate = DispatchCandidate.from(driver, tightDeadlineShipment, now, 0.1, "CLEAR");

        DeliveryDeadlineConstraint constraint = new DeliveryDeadlineConstraint();
        Optional<ConstraintViolation> violation = constraint.evaluate(candidate, context);

        assertThat(violation).isPresent();
        assertThat(violation.get().constraintId()).isEqualTo(DeliveryDeadlineConstraint.ID);
    }
}
