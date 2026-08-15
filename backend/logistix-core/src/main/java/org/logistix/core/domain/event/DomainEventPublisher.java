package org.logistix.core.domain.event;

/**
 * Outbound port for broadcasting domain events across internal boundaries or external message brokers.
 */
public interface DomainEventPublisher {

    void publish(DomainEvent event);
}
