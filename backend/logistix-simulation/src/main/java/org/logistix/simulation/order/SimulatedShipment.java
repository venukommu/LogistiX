package org.logistix.simulation.order;

import org.logistix.common.enums.PriorityLevel;
import org.logistix.common.model.Coordinates;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable synthetic shipment order model for simulation.
 */
public record SimulatedShipment(
        UUID shipmentId,
        Coordinates origin,
        Coordinates destination,
        double weightKg,
        PriorityLevel priority,
        Instant readyTime,
        Instant deadline
) {}
