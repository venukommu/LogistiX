package org.logistix.core.port.inbound;

import org.logistix.common.model.Coordinates;
import org.logistix.common.model.EntityId;
import org.logistix.core.domain.model.Location;
import org.logistix.core.domain.model.Route;
import org.logistix.core.domain.model.TimeWindow;

import java.util.List;
import java.util.UUID;

/**
 * Inbound port for multi-stop route optimization and ETA prediction.
 */
public interface RouteOptimizationUseCase {

    record StopRequest(
            Location location,
            TimeWindow preferredWindow,
            int priority
    ) {}

    record RouteOptimizationQuery(
            Coordinates origin,
            List<StopRequest> stops,
            EntityId<UUID> vehicleId,
            boolean minimizeCarbonFootprint
    ) {}

    Route calculateOptimalRoute(RouteOptimizationQuery query);
}
