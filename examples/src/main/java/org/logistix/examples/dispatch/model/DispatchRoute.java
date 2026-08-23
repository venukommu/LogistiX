package org.logistix.examples.dispatch.model;

import org.logistix.common.model.Coordinates;

import java.time.Duration;
import java.util.Objects;

/**
 * Route calculation details between driver, origin, and destination.
 */
public record DispatchRoute(
        Coordinates origin,
        Coordinates destination,
        double distanceKm,
        Duration estimatedTransitTime,
        double tollCostUsd,
        double trafficCongestionIndex,
        String weatherCondition
) {
    public DispatchRoute {
        Objects.requireNonNull(origin, "Origin must not be null");
        Objects.requireNonNull(destination, "Destination must not be null");
        Objects.requireNonNull(estimatedTransitTime, "Estimated transit time must not be null");
        weatherCondition = weatherCondition != null ? weatherCondition : "CLEAR";
        if (trafficCongestionIndex < 0.0 || trafficCongestionIndex > 1.0) {
            throw new IllegalArgumentException("Traffic congestion index must be between 0.0 and 1.0");
        }
    }

    /**
     * Compute great circle distance in kilometers using the Haversine formula.
     */
    public static double haversineDistanceKm(Coordinates from, Coordinates to) {
        final double EARTH_RADIUS_KM = 6371.0;
        double lat1 = Math.toRadians(from.latitude());
        double lon1 = Math.toRadians(from.longitude());
        double lat2 = Math.toRadians(to.latitude());
        double lon2 = Math.toRadians(to.longitude());

        double dlat = lat2 - lat1;
        double dlon = lon2 - lon1;

        double a = Math.sin(dlat / 2) * Math.sin(dlat / 2)
                + Math.cos(lat1) * Math.cos(lat2)
                * Math.sin(dlon / 2) * Math.sin(dlon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
    }

    /**
     * Estimate route parameters for an average commercial freight speed of 75 km/h.
     */
    public static DispatchRoute estimate(Coordinates from, Coordinates to, double trafficIndex, String weather) {
        double distanceKm = haversineDistanceKm(from, to);
        double speedKmH = 75.0 * (1.0 - (trafficIndex * 0.4)); // slow down up to 40% with heavy traffic
        long seconds = (long) ((distanceKm / Math.max(speedKmH, 20.0)) * 3600.0);
        double tolls = (distanceKm / 100.0) * 8.50; // ~$8.50 per 100km
        return new DispatchRoute(from, to, distanceKm, Duration.ofSeconds(seconds), tolls, trafficIndex, weather);
    }
}
