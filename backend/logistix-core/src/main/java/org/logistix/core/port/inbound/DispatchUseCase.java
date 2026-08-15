package org.logistix.core.port.inbound;

import org.logistix.common.model.EntityId;
import org.logistix.core.domain.model.Shipment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Primary inbound use case contract for AI-assisted dispatch operations.
 */
public interface DispatchUseCase {

    record DispatchCommand(
            EntityId<UUID> shipmentId,
            List<EntityId<UUID>> preferredDriverIds,
            boolean autoConfirm,
            String customConstraints
    ) {}

    record DispatchPlan(
            EntityId<UUID> dispatchId,
            EntityId<UUID> shipmentId,
            EntityId<UUID> selectedDriverId,
            EntityId<UUID> selectedVehicleId,
            double matchScore,
            String explanation,
            boolean isConfirmed
    ) {}

    DispatchPlan planDispatch(DispatchCommand command);

    Optional<DispatchPlan> getDispatchPlan(EntityId<UUID> dispatchId);

    DispatchPlan confirmDispatch(EntityId<UUID> dispatchId);
}
