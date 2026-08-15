package org.logistix.core.port.outbound;

import org.logistix.common.enums.Status;
import org.logistix.common.model.Coordinates;
import org.logistix.common.model.EntityId;
import org.logistix.common.model.PaginationRequest;
import org.logistix.common.model.PaginationResult;
import org.logistix.core.domain.model.Driver;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Outbound SPI for driver persistence and spatial lookup.
 */
public interface DriverRepositoryPort {

    Driver save(Driver driver);

    Optional<Driver> findById(EntityId<UUID> id);

    PaginationResult<Driver> findAll(PaginationRequest paginationRequest);

    List<Driver> findNearbyAvailable(Coordinates center, double radiusKm, Status status);
}
