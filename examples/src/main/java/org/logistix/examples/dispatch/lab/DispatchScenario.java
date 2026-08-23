package org.logistix.examples.dispatch.lab;

import org.logistix.examples.dispatch.model.Driver;
import org.logistix.examples.dispatch.model.Shipment;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Immutable scenario definition for repeatable dispatch experiments in the Decision Lab.
 * Contains identical shipment requirements, fleet pool, and operational corridor context.
 */
public record DispatchScenario(
        String scenarioId,
        String name,
        String description,
        Shipment shipment,
        List<Driver> candidateDrivers,
        String weatherAdvisory,
        String trafficRiskLevel,
        String corridorNotes,
        Instant evaluationTime,
        String expectedOutcomeDescription
) {
    public DispatchScenario {
        Objects.requireNonNull(scenarioId, "scenarioId must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(shipment, "shipment must not be null");
        Objects.requireNonNull(candidateDrivers, "candidateDrivers must not be null");
        weatherAdvisory = weatherAdvisory != null ? weatherAdvisory : "CLEAR";
        trafficRiskLevel = trafficRiskLevel != null ? trafficRiskLevel : "LOW";
        corridorNotes = corridorNotes != null ? corridorNotes : "Standard highway corridor";
        evaluationTime = evaluationTime != null ? evaluationTime : Instant.now();
        expectedOutcomeDescription = expectedOutcomeDescription != null ? expectedOutcomeDescription : "";
        candidateDrivers = List.copyOf(candidateDrivers);
    }
}
