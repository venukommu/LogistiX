package org.logistix.core.domain.model;

import org.logistix.common.model.EntityId;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Core Domain Entity representing an optimized transportation route.
 */
public record Route(
        EntityId<UUID> id,
        EntityId<UUID> driverId,
        EntityId<UUID> vehicleId,
        List<RouteWaypoint> waypoints,
        double totalDistanceKm,
        Duration estimatedDuration,
        RouteStatus status
) {
    public enum RouteStatus {
        PLANNED,
        EN_ROUTE,
        COMPLETED,
        ABORTED
    }

    public Route {
        Objects.requireNonNull(id, "Route ID must not be null");
        Objects.requireNonNull(status, "Status must not be null");
        waypoints = waypoints != null ? List.copyOf(waypoints) : Collections.emptyList();
    }
}
