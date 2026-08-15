package org.logistix.core.domain.event;

import org.logistix.common.model.EntityId;

import java.time.Instant;
import java.util.UUID;

/**
 * Event fired when a driver has been matched and assigned to a dispatch payload.
 */
public record DriverAssignedEvent(
        UUID eventId,
        Instant occurredOn,
        EntityId<UUID> shipmentId,
        EntityId<UUID> driverId,
        String explanationSummary
) implements DomainEvent {

    public DriverAssignedEvent(EntityId<UUID> shipmentId, EntityId<UUID> driverId, String explanationSummary) {
        this(UUID.randomUUID(), Instant.now(), shipmentId, driverId, explanationSummary);
    }

    @Override
    public String eventType() {
        return "DriverAssigned";
    }
}
