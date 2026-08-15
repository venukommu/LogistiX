package org.logistix.decision.engine;

import org.logistix.core.domain.model.Driver;
import org.logistix.core.domain.model.Shipment;
import org.logistix.decision.explainability.ExplainableRecommendation;

import java.util.List;

/**
 * Specialized decision engine interface for AI-assisted driver dispatch selection.
 */
public interface DispatchDecisionEngine extends DecisionEngine<Shipment, Driver> {

    List<ExplainableRecommendation<Driver>> rankDriversForShipment(
            DecisionContext<Shipment> context,
            List<Driver> candidateDrivers
    );
}
