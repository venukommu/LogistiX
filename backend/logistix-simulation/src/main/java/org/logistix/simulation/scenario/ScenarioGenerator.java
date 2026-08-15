package org.logistix.simulation.scenario;

import org.logistix.common.model.Coordinates;

/**
 * Contract for creating complex synthetic benchmark scenarios.
 */
public interface ScenarioGenerator {

    BenchmarkScenario generateScenario(String name, int driverCount, int shipmentCount, Coordinates regionCenter);
}
