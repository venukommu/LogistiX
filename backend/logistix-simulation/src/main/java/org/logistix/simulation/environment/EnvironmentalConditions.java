package org.logistix.simulation.environment;

import org.logistix.common.model.Coordinates;

/**
 * Immutable environmental snapshot model for simulation.
 */
public record EnvironmentalConditions(
        Coordinates location,
        WeatherCondition weather,
        TrafficCongestion traffic,
        double temperatureCelsius,
        double transitDelayFactor
) {
    public enum WeatherCondition {
        CLEAR,
        RAIN,
        HEAVY_SNOW,
        FOG,
        STORM
    }

    public enum TrafficCongestion {
        FREE_FLOW,
        MODERATE,
        HEAVY,
        GRIDLOCK
    }
}
