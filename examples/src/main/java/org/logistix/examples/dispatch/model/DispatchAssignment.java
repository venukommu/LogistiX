package org.logistix.examples.dispatch.model;

import org.logistix.domain.explanation.Explanation;
import org.logistix.domain.score.Score;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable outcome of a resolved Driver Dispatch operational decision.
 */
public record DispatchAssignment(
        UUID assignmentId,
        UUID shipmentId,
        UUID driverId,
        String driverName,
        double deadheadDistanceKm,
        double mainDistanceKm,
        Instant scheduledPickupTime,
        Instant scheduledDeliveryTime,
        double estimatedCostUsd,
        Score score,
        String rationale
) {
    public DispatchAssignment {
        Objects.requireNonNull(assignmentId, "Assignment ID must not be null");
        Objects.requireNonNull(shipmentId, "Shipment ID must not be null");
        Objects.requireNonNull(driverId, "Driver ID must not be null");
        Objects.requireNonNull(driverName, "Driver name must not be null");
        Objects.requireNonNull(scheduledPickupTime, "Pickup time must not be null");
        Objects.requireNonNull(scheduledDeliveryTime, "Delivery time must not be null");
        Objects.requireNonNull(score, "Score must not be null");
        Objects.requireNonNull(rationale, "Rationale must not be null");
    }

    public static DispatchAssignment from(DispatchCandidate candidate, String rationale) {
        return new DispatchAssignment(
                UUID.randomUUID(),
                candidate.shipment().shipmentId(),
                candidate.driver().driverId(),
                candidate.driver().name(),
                candidate.deadheadDistanceKm(),
                candidate.mainDistanceKm(),
                candidate.estimatedPickupTime(),
                candidate.estimatedDeliveryTime(),
                candidate.estimatedTotalCostUsd(),
                candidate.score(),
                rationale
        );
    }

    public static DispatchAssignment unassigned(UUID shipmentId, String reason) {
        return new DispatchAssignment(
                UUID.randomUUID(),
                shipmentId != null ? shipmentId : UUID.randomUUID(),
                UUID.fromString("00000000-0000-0000-0000-000000000000"),
                "UNASSIGNED",
                0.0,
                0.0,
                Instant.now(),
                Instant.now(),
                0.0,
                Score.of(0.0, 0.0),
                reason
        );
    }

    public boolean isAssigned() {
        return !"UNASSIGNED".equals(driverName);
    }
}
