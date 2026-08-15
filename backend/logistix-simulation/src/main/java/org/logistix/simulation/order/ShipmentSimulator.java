package org.logistix.simulation.order;

import org.logistix.common.model.Coordinates;

import java.util.List;

/**
 * Contract for synthetic order generation and demand stream simulation.
 */
public interface ShipmentSimulator {

    List<SimulatedShipment> generateOrders(int orderCount, Coordinates originCluster, Coordinates destinationCluster);
}
