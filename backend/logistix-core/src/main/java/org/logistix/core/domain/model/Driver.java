package org.logistix.core.domain.model;

import org.logistix.common.enums.Status;
import org.logistix.common.model.Coordinates;
import org.logistix.common.model.EntityId;

import java.time.Duration;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Core Domain Entity representing a commercial or fleet driver.
 */
public record Driver(
        EntityId<UUID> id,
        String licenseNumber,
        String firstName,
        String lastName,
        Coordinates currentLocation,
        Status status,
        Duration remainingDrivingHours,
        Set<String> endorsements,
        double rating
) {
    public Driver {
        Objects.requireNonNull(id, "Driver ID must not be null");
        Objects.requireNonNull(licenseNumber, "License number must not be null");
        Objects.requireNonNull(status, "Status must not be null");
        endorsements = endorsements != null ? Set.copyOf(endorsements) : Set.of();
    }
}
