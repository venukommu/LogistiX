package org.logistix.simulation.engine;

import org.logistix.simulation.scenario.BenchmarkScenario;

/**
 * Primary engine contract for executing discrete-event simulation scenarios.
 */
public interface SimulationEngine {

    SimulationResult runSimulation(BenchmarkScenario scenario);
}
