package org.logistix.core.domain.event;

import org.logistix.common.model.EntityId;

import java.time.Instant;
import java.util.UUID;

/**
 * Event fired when a dispatch lifecycle is completed.
 */
public record DispatchCompletedEvent(
        UUID eventId,
        Instant occurredOn,
        EntityId<UUID> dispatchId,
        EntityId<UUID> routeId
) implements DomainEvent {

    public DispatchCompletedEvent(EntityId<UUID> dispatchId, EntityId<UUID> routeId) {
        this(UUID.randomUUID(), Instant.now(), dispatchId, routeId);
    }

    @Override
    public String eventType() {
        return "DispatchCompleted";
    }
}
