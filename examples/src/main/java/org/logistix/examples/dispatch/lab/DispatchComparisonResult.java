package org.logistix.examples.dispatch.lab;

import org.logistix.ai.dispatch.AITelemetry;
import org.logistix.domain.decision.DecisionResult;
import org.logistix.examples.dispatch.model.DispatchAssignment;
import org.logistix.rag.knowledge.KnowledgeTelemetry;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Strongly typed side-by-side comparison result quantifying the differences between RULES_ONLY and HYBRID_AI runs,
 * including enterprise knowledge retrieval telemetry, citations, and grounded decision policy outcomes.
 */
public record DispatchComparisonResult(
        DispatchScenario scenario,
        DecisionResult<DispatchAssignment> rulesOnlyResult,
        DecisionResult<DispatchAssignment> hybridResult,
        boolean recommendationChanged,
        String previousRecommendation,
        String finalRecommendation,
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
        boolean aiInfluencedDecision,
        String aiInfluenceReason,
        String safetyStatus,
        boolean hardConstraintsSatisfied,
        boolean fallbackTriggered,
        String knowledgeProvider,
        int knowledgeEvidenceCount,
        List<String> knowledgeEvidenceIds,
        String knowledgeStatus,
        Duration knowledgeLatency
) {
    public DispatchComparisonResult {
        Objects.requireNonNull(scenario, "scenario must not be null");
        Objects.requireNonNull(rulesOnlyResult, "rulesOnlyResult must not be null");
        Objects.requireNonNull(hybridResult, "hybridResult must not be null");
        aiInsights = aiInsights != null ? List.copyOf(aiInsights) : Collections.emptyList();
        knowledgeEvidenceIds = knowledgeEvidenceIds != null ? List.copyOf(knowledgeEvidenceIds) : Collections.emptyList();
        knowledgeProvider = knowledgeProvider != null ? knowledgeProvider : "NONE";
        knowledgeStatus = knowledgeStatus != null ? knowledgeStatus : "SKIPPED";
        knowledgeLatency = knowledgeLatency != null ? knowledgeLatency : Duration.ZERO;
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

        KnowledgeTelemetry kTelemetry = (KnowledgeTelemetry) hybrid.recommendation().metadata().get("knowledgeTelemetry");
        String kProvider = kTelemetry != null ? kTelemetry.providerName() : "NONE";
        int kEvidenceCount = kTelemetry != null ? kTelemetry.retrievedCount() : 0;
        List<String> kEvidenceIds = kTelemetry != null ? kTelemetry.evidenceDocumentIds() : Collections.emptyList();
        String kStatus = kTelemetry != null ? kTelemetry.status() : "SKIPPED";
        Duration kLatency = kTelemetry != null ? kTelemetry.retrievalLatency() : Duration.ZERO;

        boolean aiInfluenced = Boolean.TRUE.equals(hybrid.recommendation().metadata().get("aiInfluencedDecision"));
        String influenceReason = (String) hybrid.recommendation().metadata().getOrDefault("aiInfluenceReason",
                changed ? "AI contextual risk signals shifted the deterministic selection policy." : "AI confirmed deterministic recommendation.");

        List<String> insights = hybrid.explanation() != null
                ? hybrid.explanation().keyFactors().stream().filter(f -> f.startsWith("AI Context") || f.startsWith("AI Decision Policy") || f.startsWith("Knowledge Evidence")).toList()
                : Collections.emptyList();

        double advConf = telemetry != null && telemetry.advisoryConfidence() != null ? telemetry.advisoryConfidence() : 0.0;
        double roConf = rulesOnly.score() != null ? rulesOnly.score().confidence() : 0.0;
        double hyConf = hybrid.score() != null ? hybrid.score().confidence() : 0.0;

        boolean hardSatisfied = hybrid.recommendation().item() != null && hybrid.recommendation().item().isAssigned();
        String safetyStatus = fallback ? "FALLBACK" : hardSatisfied ? "SAFE" : "ERROR";

        return new DispatchComparisonResult(
                scenario,
                rulesOnly,
                hybrid,
                changed,
                roDriver,
                hyDriver,
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
                aiInfluenced,
                influenceReason,
                safetyStatus,
                hardSatisfied,
                fallback,
                kProvider,
                kEvidenceCount,
                kEvidenceIds,
                kStatus,
                kLatency
        );
    }
}
