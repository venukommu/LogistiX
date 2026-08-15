package org.logistix.simulation.environment;

import org.logistix.common.model.Coordinates;

/**
 * Contract for synthetic weather event and impact simulation.
 */
public interface WeatherSimulator {

    EnvironmentalConditions.WeatherCondition simulateWeather(Coordinates location);

    double getSpeedPenaltyForWeather(EnvironmentalConditions.WeatherCondition weather);
}
