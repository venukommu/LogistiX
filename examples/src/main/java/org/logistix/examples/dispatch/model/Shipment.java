package org.logistix.examples.dispatch.model;

import org.logistix.common.enums.PriorityLevel;
import org.logistix.common.model.Coordinates;

import java.time.Instant;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Immutable domain entity representing a commercial freight or courier shipment order.
 */
public record Shipment(
        UUID shipmentId,
        Coordinates origin,
        Coordinates destination,
        double weightKg,
        double volumeM3,
        Set<Certification> requiredCertifications,
        Instant pickupTimeWindowStart,
        Instant pickupTimeWindowEnd,
        Instant deliveryDeadline,
        PriorityLevel priority,
        String destinationRegion
) {
    public Shipment {
        Objects.requireNonNull(shipmentId, "Shipment ID must not be null");
        Objects.requireNonNull(origin, "Origin coordinates must not be null");
        Objects.requireNonNull(destination, "Destination coordinates must not be null");
        Objects.requireNonNull(deliveryDeadline, "Delivery deadline must not be null");
        requiredCertifications = requiredCertifications != null ? Set.copyOf(requiredCertifications) : Collections.emptySet();
        priority = priority != null ? priority : PriorityLevel.MEDIUM;
        destinationRegion = destinationRegion != null ? destinationRegion : "UNKNOWN";
        if (weightKg <= 0) {
            throw new IllegalArgumentException("Weight must be positive");
        }
        if (volumeM3 <= 0) {
            throw new IllegalArgumentException("Volume must be positive");
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID shipmentId = UUID.randomUUID();
        private Coordinates origin = Coordinates.of(37.7749, -122.4194);
        private Coordinates destination = Coordinates.of(34.0522, -118.2437);
        private double weightKg = 5000.0;
        private double volumeM3 = 20.0;
        private Set<Certification> requiredCertifications = Collections.emptySet();
        private Instant pickupTimeWindowStart = Instant.now();
        private Instant pickupTimeWindowEnd = Instant.now().plusSeconds(7200);
        private Instant deliveryDeadline = Instant.now().plusSeconds(28800);
        private PriorityLevel priority = PriorityLevel.HIGH;
        private String destinationRegion = "US-WEST";

        public Builder shipmentId(UUID shipmentId) { this.shipmentId = shipmentId; return this; }
        public Builder origin(Coordinates origin) { this.origin = origin; return this; }
        public Builder destination(Coordinates destination) { this.destination = destination; return this; }
        public Builder weightKg(double weightKg) { this.weightKg = weightKg; return this; }
        public Builder volumeM3(double volumeM3) { this.volumeM3 = volumeM3; return this; }
        public Builder requiredCertifications(Set<Certification> certs) { this.requiredCertifications = certs; return this; }
        public Builder pickupTimeWindowStart(Instant start) { this.pickupTimeWindowStart = start; return this; }
        public Builder pickupTimeWindowEnd(Instant end) { this.pickupTimeWindowEnd = end; return this; }
        public Builder deliveryDeadline(Instant deadline) { this.deliveryDeadline = deadline; return this; }
        public Builder priority(PriorityLevel priority) { this.priority = priority; return this; }
        public Builder destinationRegion(String region) { this.destinationRegion = region; return this; }

        public Shipment build() {
            return new Shipment(shipmentId, origin, destination, weightKg, volumeM3, requiredCertifications,
                    pickupTimeWindowStart, pickupTimeWindowEnd, deliveryDeadline, priority, destinationRegion);
        }
    }
}
