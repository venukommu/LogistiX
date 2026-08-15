package org.logistix.domain.events;

import org.logistix.domain.decision.DecisionResult;

import java.time.Instant;
import java.util.UUID;

/**
 * Event fired upon successful completion of a decision execution flow.
 *
 * @param <T> Decision payload result type
 */
public record DecisionCompletedEvent<T>(
        UUID eventId,
        Instant occurredOn,
        DecisionResult<T> result
) implements DomainEvent {

    public DecisionCompletedEvent(DecisionResult<T> result) {
        this(UUID.randomUUID(), Instant.now(), result);
    }

    @Override
    public String eventType() {
        return "DecisionCompleted";
    }
}
