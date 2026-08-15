package org.logistix.core.domain.model;

import org.logistix.common.model.Coordinates;

import java.time.Instant;
import java.util.Objects;

/**
 * An individual waypoint or stop along an optimized route.
 */
public record RouteWaypoint(
        int sequence,
        Location location,
        WaypointType type,
        TimeWindow scheduledWindow,
        Instant estimatedArrivalTime
) {
    public enum WaypointType {
        PICKUP,
        DELIVERY,
        REST_STOP,
        FUEL_STOP
    }

    public RouteWaypoint {
        Objects.requireNonNull(location, "Location cannot be null");
        Objects.requireNonNull(type, "Waypoint type cannot be null");
    }
}
