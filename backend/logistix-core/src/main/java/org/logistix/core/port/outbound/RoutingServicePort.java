package org.logistix.core.port.outbound;

import org.logistix.common.model.Coordinates;

import java.time.Duration;
import java.util.List;

/**
 * Outbound SPI for calculating matrix distances and transit durations.
 */
public interface RoutingServicePort {

    record DistanceMatrixResult(
            double distanceKm,
            Duration estimatedDuration
    ) {}

    DistanceMatrixResult calculateDistanceAndDuration(Coordinates origin, Coordinates destination);

    List<Coordinates> computeRoutePolyline(Coordinates origin, Coordinates destination, List<Coordinates> waypoints);
}
