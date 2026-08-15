package org.logistix.core.domain.model;

import org.logistix.common.enums.Status;
import org.logistix.common.model.EntityId;

import java.util.Objects;
import java.util.UUID;

/**
 * Core Domain Entity representing a fleet vehicle or trailer.
 */
public record Vehicle(
        EntityId<UUID> id,
        String vin,
        String licensePlate,
        VehicleType type,
        Weight maxPayloadCapacity,
        Dimensions cargoDimensions,
        Status status
) {
    public enum VehicleType {
        SEMI_TRUCK,
        BOX_TRUCK,
        CARGO_VAN,
        FLATBED,
        REFRIGERATED
    }

    public Vehicle {
        Objects.requireNonNull(id, "Vehicle ID must not be null");
        Objects.requireNonNull(vin, "VIN must not be null");
        Objects.requireNonNull(type, "Vehicle type must not be null");
        Objects.requireNonNull(status, "Status must not be null");
    }
}
