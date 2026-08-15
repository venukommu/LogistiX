package org.logistix.simulation.engine;

import org.logistix.simulation.scenario.BenchmarkScenario;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

/**
 * Immutable outcome of a discrete-event simulation run.
 */
public record SimulationResult(
        UUID runId,
        BenchmarkScenario scenario,
        int totalDispatchesAttempted,
        int successfulDeliveries,
        int onTimeDeliveries,
        double totalCostIncurred,
        double averageConfidenceScore,
        Duration simulationWallTime,
        Map<String, Object> metrics
) {
    public SimulationResult {
        metrics = metrics != null ? Map.copyOf(metrics) : Collections.emptyMap();
    }
}
