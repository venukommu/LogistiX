package org.logistix.core.port.outbound;

import org.logistix.common.model.EntityId;
import org.logistix.common.model.PaginationRequest;
import org.logistix.common.model.PaginationResult;
import org.logistix.core.domain.model.Shipment;

import java.util.Optional;
import java.util.UUID;

/**
 * Outbound SPI for shipment persistence.
 */
public interface ShipmentRepositoryPort {

    Shipment save(Shipment shipment);

    Optional<Shipment> findById(EntityId<UUID> id);

    PaginationResult<Shipment> findAll(PaginationRequest paginationRequest);

    void deleteById(EntityId<UUID> id);
}
