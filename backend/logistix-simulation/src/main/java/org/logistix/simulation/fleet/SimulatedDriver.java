package org.logistix.simulation.fleet;

import org.logistix.common.model.Coordinates;

import java.time.Duration;
import java.util.UUID;

/**
 * Immutable synthetic driver state model for simulation runs.
 */
public record SimulatedDriver(
        UUID driverId,
        String name,
        Coordinates initialLocation,
        Duration remainingDrivingHours,
        double speedFactor,
        double reliabilityScore
) {}
