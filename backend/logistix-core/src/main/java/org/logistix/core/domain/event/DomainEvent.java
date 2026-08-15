package org.logistix.core.domain.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Base interface for all immutable domain events occurring within the LogistiX core.
 */
public interface DomainEvent {

    UUID eventId();

    Instant occurredOn();

    String eventType();
}
