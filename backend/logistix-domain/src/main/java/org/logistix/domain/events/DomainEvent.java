package org.logistix.domain.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Base contract for immutable domain events emitted during operational decision flows.
 */
public interface DomainEvent {

    UUID eventId();

    Instant occurredOn();

    String eventType();
}
