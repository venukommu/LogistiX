package org.logistix.examples.dispatch.lab;

import org.logistix.domain.decision.DecisionContext;
import org.logistix.domain.fact.Fact;
import org.logistix.domain.fact.FactBag;
import org.logistix.examples.dispatch.model.DispatchCandidate;
import org.logistix.examples.dispatch.pipeline.DispatchDecisionPipelineFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable wrapper guaranteeing identical scenario input parameters across both RULES_ONLY and HYBRID_AI runs.
 */
public record DispatchComparisonInput(
        DispatchScenario scenario,
        List<DispatchCandidate> preparedCandidates
) {
    public DispatchComparisonInput {
        Objects.requireNonNull(scenario, "scenario must not be null");
        Objects.requireNonNull(preparedCandidates, "preparedCandidates must not be null");
        preparedCandidates = List.copyOf(preparedCandidates);
    }

    public static DispatchComparisonInput from(DispatchScenario scenario) {
        List<DispatchCandidate> candidates = scenario.candidateDrivers().stream()
                .map(d -> DispatchCandidate.from(
                        d,
                        scenario.shipment(),
                        scenario.evaluationTime(),
                        0.10,
                        scenario.weatherAdvisory()
                ))
                .toList();

        return new DispatchComparisonInput(scenario, candidates);
    }

    /**
     * Builds the DecisionContext for the given execution mode.
     */
    public DecisionContext toDecisionContext(DispatchDecisionMode mode, String correlationId) {
        return DecisionContext.of(
                DispatchDecisionPipelineFactory.DECISION_TYPE,
                FactBag.of(
                        Fact.of("candidates", preparedCandidates),
                        Fact.of("shipment", scenario.shipment())
                ),
                Map.of(
                        "weatherAdvisory", scenario.weatherAdvisory(),
                        "trafficRiskLevel", scenario.trafficRiskLevel(),
                        "corridorNotes", scenario.corridorNotes()
                ),
                Map.of(
                        "executionMode", mode.name(),
                        "correlationId", correlationId != null ? correlationId : scenario.scenarioId() + "-" + mode.name()
                )
        );
    }
}
