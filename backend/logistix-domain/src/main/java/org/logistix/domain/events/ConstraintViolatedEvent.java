package org.logistix.domain.events;

import org.logistix.domain.constraint.ConstraintViolation;

import java.time.Instant;
import java.util.UUID;

/**
 * Event fired when a candidate evaluation triggers a constraint violation.
 */
public record ConstraintViolatedEvent(
        UUID eventId,
        Instant occurredOn,
        UUID contextId,
        ConstraintViolation violation
) implements DomainEvent {

    public ConstraintViolatedEvent(UUID contextId, ConstraintViolation violation) {
        this(UUID.randomUUID(), Instant.now(), contextId, violation);
    }

    @Override
    public String eventType() {
        return "ConstraintViolated";
    }
}
