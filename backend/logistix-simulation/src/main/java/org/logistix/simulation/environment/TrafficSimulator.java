package org.logistix.simulation.environment;

import org.logistix.common.model.Coordinates;

import java.time.Instant;

/**
 * Contract for synthetic traffic pattern and congestion simulation.
 */
public interface TrafficSimulator {

    EnvironmentalConditions.TrafficCongestion simulateTraffic(Coordinates origin, Coordinates destination, Instant timestamp);

    double getTransitDelayFactor(EnvironmentalConditions.TrafficCongestion congestion);
}
