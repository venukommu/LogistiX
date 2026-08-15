package org.logistix.domain.events;

import org.logistix.domain.decision.DecisionMetadata;

import java.time.Instant;
import java.util.UUID;

/**
 * Event fired when a new decision request has entered the pipeline.
 */
public record DecisionRequestedEvent(
        UUID eventId,
        Instant occurredOn,
        UUID contextId,
        String decisionType,
        DecisionMetadata metadata
) implements DomainEvent {

    public DecisionRequestedEvent(UUID contextId, String decisionType, DecisionMetadata metadata) {
        this(UUID.randomUUID(), Instant.now(), contextId, decisionType, metadata);
    }

    @Override
    public String eventType() {
        return "DecisionRequested";
    }
}
