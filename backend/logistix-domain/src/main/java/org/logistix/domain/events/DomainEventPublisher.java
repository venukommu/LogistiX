package org.logistix.domain.events;

/**
 * Outbound port for broadcasting domain events across framework modules or external message brokers.
 */
public interface DomainEventPublisher {

    void publish(DomainEvent event);
}
