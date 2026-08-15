package org.logistix.core.domain.event;

import org.logistix.common.model.EntityId;

import java.time.Instant;
import java.util.UUID;

/**
 * Event fired when a new shipment has been registered in the system.
 */
public record ShipmentCreatedEvent(
        UUID eventId,
        Instant occurredOn,
        EntityId<UUID> shipmentId,
        String trackingNumber
) implements DomainEvent {

    public ShipmentCreatedEvent(EntityId<UUID> shipmentId, String trackingNumber) {
        this(UUID.randomUUID(), Instant.now(), shipmentId, trackingNumber);
    }

    @Override
    public String eventType() {
        return "ShipmentCreated";
    }
}
