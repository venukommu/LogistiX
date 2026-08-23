package org.logistix.examples.dispatch.constraints;

import org.logistix.domain.constraint.Constraint;
import org.logistix.domain.constraint.ConstraintSeverity;
import org.logistix.domain.constraint.ConstraintViolation;
import org.logistix.domain.decision.DecisionContext;
import org.logistix.examples.dispatch.model.DispatchCandidate;

import java.util.Map;
import java.util.Optional;

/**
 * Hard constraint ensuring the candidate driver's vehicle payload capacity can accommodate
 * both the gross weight and volume of the shipment.
 */
public class VehicleCapacityConstraint implements Constraint<DispatchCandidate> {

    public static final String ID = "CONSTRAINT_VEHICLE_CAPACITY";
    public static final String NAME = "Vehicle Payload Capacity";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public ConstraintSeverity getSeverity() {
        return ConstraintSeverity.HARD;
    }

    @Override
    public Optional<ConstraintViolation> evaluate(DispatchCandidate candidate, DecisionContext context) {
        double maxWeight = candidate.driver().vehicleWeightCapacityKg();
        double shipWeight = candidate.shipment().weightKg();
        double maxVol = candidate.driver().vehicleVolumeCapacityM3();
        double shipVol = candidate.shipment().volumeM3();

        if (shipWeight > maxWeight) {
            return Optional.of(new ConstraintViolation(
                    ID,
                    NAME,
                    ConstraintSeverity.HARD,
                    String.format("Vehicle weight capacity exceeded: %s capacity is %.1f kg, but shipment requires %.1f kg",
                            candidate.driver().name(), maxWeight, shipWeight),
                    Map.of("driverWeightCapacityKg", maxWeight, "shipmentWeightKg", shipWeight),
                    context.timestamp()
            ));
        }

        if (shipVol > maxVol) {
            return Optional.of(new ConstraintViolation(
                    ID,
                    NAME,
                    ConstraintSeverity.HARD,
                    String.format("Vehicle volume capacity exceeded: %s capacity is %.1f m3, but shipment requires %.1f m3",
                            candidate.driver().name(), maxVol, shipVol),
                    Map.of("driverVolumeCapacityM3", maxVol, "shipmentVolumeM3", shipVol),
                    context.timestamp()
            ));
        }

        return Optional.empty();
    }
}
