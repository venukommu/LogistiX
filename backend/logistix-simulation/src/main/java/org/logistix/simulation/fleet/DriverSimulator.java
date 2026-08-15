package org.logistix.simulation.fleet;

import org.logistix.common.model.Coordinates;

import java.util.List;

/**
 * Contract for generating and evolving synthetic driver fleets.
 */
public interface DriverSimulator {

    List<SimulatedDriver> generateFleet(int count, Coordinates geographicCenter, double radiusKm);

    SimulatedDriver advanceDriverState(SimulatedDriver current, double elapsedHours);
}
