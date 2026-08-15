package org.logistix.core.port.inbound;

import org.logistix.common.model.EntityId;
import org.logistix.common.model.PaginationRequest;
import org.logistix.common.model.PaginationResult;
import org.logistix.core.domain.model.Shipment;

import java.util.Optional;
import java.util.UUID;

/**
 * Inbound port for shipment lifecycle management.
 */
public interface ShipmentManagementUseCase {

    Shipment createShipment(Shipment shipment);

    Optional<Shipment> getShipment(EntityId<UUID> id);

    PaginationResult<Shipment> listShipments(PaginationRequest paginationRequest);

    Shipment updateShipment(Shipment shipment);

    void cancelShipment(EntityId<UUID> id);
}
