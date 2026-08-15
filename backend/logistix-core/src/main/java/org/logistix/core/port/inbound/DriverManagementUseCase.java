package org.logistix.core.port.inbound;

import org.logistix.common.model.EntityId;
import org.logistix.common.model.PaginationRequest;
import org.logistix.common.model.PaginationResult;
import org.logistix.core.domain.model.Driver;

import java.util.Optional;
import java.util.UUID;

/**
 * Inbound port for driver registry and availability management.
 */
public interface DriverManagementUseCase {

    Driver registerDriver(Driver driver);

    Optional<Driver> getDriver(EntityId<UUID> id);

    PaginationResult<Driver> listDrivers(PaginationRequest paginationRequest);

    Driver updateDriver(Driver driver);
}
