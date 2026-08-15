package org.logistix.simulation.scenario;

import org.logistix.simulation.fleet.SimulatedDriver;
import org.logistix.simulation.order.SimulatedShipment;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Immutable synthetic scenario bundle combining drivers, shipments, and environmental constraints.
 */
public record BenchmarkScenario(
        UUID scenarioId,
        String name,
        String description,
        List<SimulatedDriver> drivers,
        List<SimulatedShipment> shipments,
        int timeHorizonHours
) {
    public BenchmarkScenario {
        drivers = drivers != null ? List.copyOf(drivers) : Collections.emptyList();
        shipments = shipments != null ? List.copyOf(shipments) : Collections.emptyList();
    }
}
