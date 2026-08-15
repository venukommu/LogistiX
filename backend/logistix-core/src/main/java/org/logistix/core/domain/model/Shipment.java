package org.logistix.core.domain.model;

import org.logistix.common.enums.PriorityLevel;
import org.logistix.common.enums.Status;
import org.logistix.common.model.EntityId;
import org.logistix.common.model.Money;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Core Domain Entity representing a freight or parcel shipment.
 */
public record Shipment(
        EntityId<UUID> id,
        String trackingNumber,
        Location origin,
        Location destination,
        TimeWindow pickupWindow,
        TimeWindow deliveryWindow,
        Weight weight,
        Dimensions dimensions,
        PriorityLevel priority,
        Status status,
        Money estimatedValue,
        Instant createdAt,
        Instant updatedAt
) {
    public Shipment {
        Objects.requireNonNull(id, "Shipment ID must not be null");
        Objects.requireNonNull(origin, "Origin must not be null");
        Objects.requireNonNull(destination, "Destination must not be null");
        Objects.requireNonNull(priority, "Priority must not be null");
        Objects.requireNonNull(status, "Status must not be null");
    }
}
