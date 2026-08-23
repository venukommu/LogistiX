package org.logistix.examples.dispatch.model;

import org.logistix.common.model.Coordinates;

import java.time.Duration;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Immutable domain entity representing an operational commercial driver in the fleet.
 */
public record Driver(
        UUID driverId,
        String name,
        Coordinates currentLocation,
        Duration remainingHos,
        double vehicleWeightCapacityKg,
        double vehicleVolumeCapacityM3,
        Set<Certification> certifications,
        DriverTier tier,
        Duration timeUntilMandatoryRest,
        String homeRegion,
        double rating,
        double historicalOnTimeRate
) {
    public Driver {
        Objects.requireNonNull(driverId, "Driver ID must not be null");
        Objects.requireNonNull(name, "Driver name must not be null");
        Objects.requireNonNull(currentLocation, "Current location must not be null");
        Objects.requireNonNull(remainingHos, "Remaining HOS must not be null");
        certifications = certifications != null ? Set.copyOf(certifications) : Collections.emptySet();
        tier = tier != null ? tier : DriverTier.STANDARD;
        timeUntilMandatoryRest = timeUntilMandatoryRest != null ? timeUntilMandatoryRest : remainingHos;
        homeRegion = homeRegion != null ? homeRegion : "UNKNOWN";
        if (rating < 0.0 || rating > 5.0) {
            throw new IllegalArgumentException("Driver rating must be between 0.0 and 5.0");
        }
        if (historicalOnTimeRate < 0.0 || historicalOnTimeRate > 1.0) {
            throw new IllegalArgumentException("Historical on-time rate must be between 0.0 and 1.0");
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID driverId = UUID.randomUUID();
        private String name = "Driver";
        private Coordinates currentLocation = Coordinates.of(37.7749, -122.4194);
        private Duration remainingHos = Duration.ofHours(8);
        private double vehicleWeightCapacityKg = 20000.0;
        private double vehicleVolumeCapacityM3 = 60.0;
        private Set<Certification> certifications = Collections.emptySet();
        private DriverTier tier = DriverTier.STANDARD;
        private Duration timeUntilMandatoryRest = Duration.ofHours(4);
        private String homeRegion = "US-WEST";
        private double rating = 4.8;
        private double historicalOnTimeRate = 0.95;

        public Builder driverId(UUID driverId) { this.driverId = driverId; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder currentLocation(Coordinates currentLocation) { this.currentLocation = currentLocation; return this; }
        public Builder remainingHos(Duration remainingHos) { this.remainingHos = remainingHos; return this; }
        public Builder vehicleWeightCapacityKg(double capacity) { this.vehicleWeightCapacityKg = capacity; return this; }
        public Builder vehicleVolumeCapacityM3(double capacity) { this.vehicleVolumeCapacityM3 = capacity; return this; }
        public Builder certifications(Set<Certification> certifications) { this.certifications = certifications; return this; }
        public Builder tier(DriverTier tier) { this.tier = tier; return this; }
        public Builder timeUntilMandatoryRest(Duration rest) { this.timeUntilMandatoryRest = rest; return this; }
        public Builder homeRegion(String homeRegion) { this.homeRegion = homeRegion; return this; }
        public Builder rating(double rating) { this.rating = rating; return this; }
        public Builder historicalOnTimeRate(double rate) { this.historicalOnTimeRate = rate; return this; }

        public Driver build() {
            return new Driver(driverId, name, currentLocation, remainingHos, vehicleWeightCapacityKg, vehicleVolumeCapacityM3, certifications, tier, timeUntilMandatoryRest, homeRegion, rating, historicalOnTimeRate);
        }
    }
}
