package org.logistix.core.port.outbound;

import org.logistix.common.model.EntityId;
import org.logistix.core.domain.model.Vehicle;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Outbound SPI for fleet vehicle persistence.
 */
public interface VehicleRepositoryPort {

    Vehicle save(Vehicle vehicle);

    Optional<Vehicle> findById(EntityId<UUID> id);

    List<Vehicle> findAvailableVehicles();
}
