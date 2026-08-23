package org.logistix.examples.dispatch.lab;

import org.logistix.ai.dispatch.AITelemetry;
import org.logistix.domain.decision.DecisionResult;
import org.logistix.examples.dispatch.model.DispatchAssignment;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Strongly typed side-by-side comparison result quantifying the differences between RULES_ONLY and HYBRID_AI runs.
 */
public record DispatchComparisonResult(
        DispatchScenario scenario,
        DecisionResult<DispatchAssignment> rulesOnlyResult,
        DecisionResult<DispatchAssignment> hybridResult,
        boolean recommendationChanged,
        String rulesOnlyDriver,
        String hybridDriver,
        double rulesOnlyScore,
        double hybridScore,
        double scoreDifference,
        int aiInvocations,
        Duration aiLatency,
        String aiProvider,
        String aiProviderType,
        List<String> aiInsights,
        double aiAdvisoryConfidence,
        double rulesOnlyConfidence,
        double hybridConfidence,
        boolean hardConstraintsSatisfied,
        boolean fallbackTriggered
) {
    public DispatchComparisonResult {
        Objects.requireNonNull(scenario, "scenario must not be null");
        Objects.requireNonNull(rulesOnlyResult, "rulesOnlyResult must not be null");
        Objects.requireNonNull(hybridResult, "hybridResult must not be null");
        aiInsights = aiInsights != null ? List.copyOf(aiInsights) : Collections.emptyList();
    }

    public static DispatchComparisonResult of(
            DispatchScenario scenario,
            DecisionResult<DispatchAssignment> rulesOnly,
            DecisionResult<DispatchAssignment> hybrid
    ) {
        String roDriver = rulesOnly.recommendation().item() != null ? rulesOnly.recommendation().item().driverName() : "UNASSIGNED";
        String hyDriver = hybrid.recommendation().item() != null ? hybrid.recommendation().item().driverName() : "UNASSIGNED";
        boolean changed = !Objects.equals(roDriver, hyDriver);

        double roScore = rulesOnly.score() != null ? rulesOnly.score().value() : 0.0;
        double hyScore = hybrid.score() != null ? hybrid.score().value() : 0.0;
        double scoreDiff = hyScore - roScore;

        AITelemetry telemetry = (AITelemetry) hybrid.recommendation().metadata().get("aiTelemetry");
        int invocations = telemetry != null ? telemetry.invocationCount() : 0;
        Duration latency = telemetry != null ? telemetry.latency() : Duration.ZERO;
        String provider = telemetry != null ? telemetry.providerName() : "NONE";
        String providerType = telemetry != null ? telemetry.providerType() : "NONE";
        boolean fallback = telemetry != null && telemetry.fallbackTriggered();

        List<String> insights = hybrid.explanation() != null
                ? hybrid.explanation().keyFactors().stream().filter(f -> f.startsWith("AI Context")).toList()
                : Collections.emptyList();

        double advConf = telemetry != null && telemetry.advisoryConfidence() != null ? telemetry.advisoryConfidence() : 0.0;
        double roConf = rulesOnly.score() != null ? rulesOnly.score().confidence() : 0.0;
        double hyConf = hybrid.score() != null ? hybrid.score().confidence() : 0.0;

        boolean hardSatisfied = hybrid.recommendation().item() != null && hybrid.recommendation().item().isAssigned();

        return new DispatchComparisonResult(
                scenario,
                rulesOnly,
                hybrid,
                changed,
                roDriver,
                hyDriver,
                roScore,
                hyScore,
                scoreDiff,
                invocations,
                latency,
                provider,
                providerType,
                insights,
                advConf,
                roConf,
                hyConf,
                hardSatisfied,
                fallback
        );
    }
}
