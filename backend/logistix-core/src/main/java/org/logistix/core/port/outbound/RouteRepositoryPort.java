package org.logistix.core.port.outbound;

import org.logistix.common.model.EntityId;
import org.logistix.core.domain.model.Route;

import java.util.Optional;
import java.util.UUID;

/**
 * Outbound SPI for route entity persistence.
 */
public interface RouteRepositoryPort {

    Route save(Route route);

    Optional<Route> findById(EntityId<UUID> id);
}
