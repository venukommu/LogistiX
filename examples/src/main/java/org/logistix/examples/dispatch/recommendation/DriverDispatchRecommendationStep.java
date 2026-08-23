package org.logistix.examples.dispatch.recommendation;

import org.logistix.ai.dispatch.AITelemetry;
import org.logistix.domain.decision.DecisionContext;
import org.logistix.domain.explanation.Explanation;
import org.logistix.domain.explanation.FeatureContribution;
import org.logistix.domain.fact.Fact;
import org.logistix.domain.ports.KnowledgeProvider.GroundingDocument;
import org.logistix.domain.recommendation.Recommendation;
import org.logistix.domain.score.Score;
import org.logistix.engine.steps.RecommendationStep;
import org.logistix.engine.steps.StepMetadata;
import org.logistix.engine.steps.StepResult;
import org.logistix.examples.dispatch.model.DispatchAssignment;
import org.logistix.examples.dispatch.model.DispatchCandidate;
import org.logistix.rag.knowledge.KnowledgeTelemetry;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Deterministic recommendation and explainability synthesis step.
 * Integrates deterministic policy evaluation for AI qualitative risk signals among close feasible candidates,
 * strictly separating deterministic factors, knowledge evidence, and AI contextual insights.
 */
public class DriverDispatchRecommendationStep implements RecommendationStep {

    public static final String STEP_ID = "step-dispatch-recommendation";
    public static final String STEP_NAME = "Driver Dispatch Recommendation & Explainability";

    @Override
    public StepMetadata getMetadata() {
        return StepMetadata.of(STEP_ID, STEP_NAME, 50);
    }

    @Override
    @SuppressWarnings("unchecked")
    public StepResult execute(DecisionContext context) {
        Instant start = Instant.now();

        List<DispatchCandidate> rankedCandidates = context.getFactValue("rankedCandidates", List.class)
                .orElse(Collections.emptyList());

        if (rankedCandidates.isEmpty()) {
            Explanation noCandidatesExp = new Explanation(
                    "No feasible drivers found meeting all hard operational constraints (HOS, capacity, certifications, deadline).",
                    0.0,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    List.of("Zero feasible drivers in pool"),
                    List.of("Consider expanding search radius or adjusting delivery SLA window.")
            );

            AITelemetry aiTelemetry = context.getFactValue("aiTelemetry", AITelemetry.class).orElse(null);
            Map<String, Object> emptyMeta = new LinkedHashMap<>();
            if (aiTelemetry != null) {
                emptyMeta.put("aiTelemetry", aiTelemetry);
                emptyMeta.put("aiEnrichmentStatus", aiTelemetry.status());
            }

            DispatchAssignment unassigned = DispatchAssignment.unassigned(null, "No feasible driver available.");
            Recommendation<DispatchAssignment> emptyRec = new Recommendation<>(
                    unassigned,
                    0,
                    Score.of(0.0, 0.0),
                    0.0,
                    "No feasible driver available.",
                    noCandidatesExp,
                    emptyMeta
            );

            DecisionContext updatedContext = context
                    .withFact(Fact.of("recommendation", emptyRec))
                    .withFact(Fact.of("explanation", noCandidatesExp));

            return StepResult.success(updatedContext, Duration.between(start, Instant.now()), "No candidates to recommend.");
        }

        // --- Deterministic Policy Evaluation for AI Qualitative Risk Signals ---
        DispatchCandidate topCandidate = rankedCandidates.get(0);
        DispatchCandidate selectedCandidate = topCandidate;
        boolean aiInfluenced = false;
        String aiInfluenceReason = "AI confirmed deterministic recommendation.";

        AITelemetry aiTelemetry = context.getFactValue("aiTelemetry", AITelemetry.class).orElse(null);
        String aiStatus = aiTelemetry != null ? aiTelemetry.status() : context.getFactValue("aiEnrichmentStatus", String.class).orElse("NOT_EXECUTED");

        if ("SUCCESS".equalsIgnoreCase(aiStatus) && rankedCandidates.size() > 1) {
            String topAiAnalysis = topCandidate.aiRiskAnalysis();
            // If top candidate has HIGH/CRITICAL risk and a close runner-up (within score threshold) has LOW risk
            if (topAiAnalysis != null && (topAiAnalysis.contains("Risk: HIGH") || topAiAnalysis.contains("Risk: CRITICAL"))) {
                for (int i = 1; i < Math.min(3, rankedCandidates.size()); i++) {
                    DispatchCandidate runnerUp = rankedCandidates.get(i);
                    String runnerUpAi = runnerUp.aiRiskAnalysis();
                    if (runnerUpAi != null && runnerUpAi.contains("Risk: LOW") && (topCandidate.score().value() - runnerUp.score().value()) <= 0.06) {
                        selectedCandidate = runnerUp;
                        aiInfluenced = true;
                        aiInfluenceReason = String.format("Severe weather risk and corridor bottleneck favored %s (LOW risk) over %s (HIGH risk).",
                                runnerUp.driver().name(), topCandidate.driver().name());
                        break;
                    }
                }
            }
        }

        DispatchCandidate best = selectedCandidate;
        Score bestScore = best.score();

        // 1. Build Feature Contributions (Deterministic)
        List<FeatureContribution> contributions = new ArrayList<>();
        Map<String, Double> subScores = bestScore.subScores();

        double proximity = subScores.getOrDefault("proximity", 0.5);
        contributions.add(new FeatureContribution(
                "Deadhead Proximity",
                0.25,
                proximity,
                proximity >= 0.7 ? FeatureContribution.ImpactDirection.POSITIVE : FeatureContribution.ImpactDirection.NEUTRAL,
                String.format("Deadhead distance is %.1f km (score: %.2f)", best.deadheadDistanceKm(), proximity)
        ));

        double etaMargin = subScores.getOrDefault("etaMargin", 0.5);
        contributions.add(new FeatureContribution(
                "ETA SLA Margin",
                0.25,
                etaMargin,
                etaMargin >= 0.6 ? FeatureContribution.ImpactDirection.POSITIVE : FeatureContribution.ImpactDirection.NEUTRAL,
                String.format("Arrives at %s with robust margin before deadline %s", best.estimatedDeliveryTime(), best.shipment().deliveryDeadline())
        ));

        double perf = subScores.getOrDefault("driverPerformance", 0.5);
        contributions.add(new FeatureContribution(
                "Driver Rating & On-Time History",
                0.20,
                perf,
                FeatureContribution.ImpactDirection.POSITIVE,
                String.format("Rating %.1f/5.0 with %.0f%% historical on-time delivery rate", best.driver().rating(), best.driver().historicalOnTimeRate() * 100.0)
        ));

        double cost = subScores.getOrDefault("costEfficiency", 0.5);
        contributions.add(new FeatureContribution(
                "Trip Cost Efficiency",
                0.15,
                cost,
                FeatureContribution.ImpactDirection.POSITIVE,
                String.format("Estimated trip cost is $%.2f", best.estimatedTotalCostUsd())
        ));

        double ruleAdj = subScores.getOrDefault("ruleAdjustments", 0.5);
        contributions.add(new FeatureContribution(
                "Business Rule Incentives",
                0.15,
                ruleAdj,
                ruleAdj >= 0.5 ? FeatureContribution.ImpactDirection.POSITIVE : FeatureContribution.ImpactDirection.NEGATIVE,
                String.format("Driver tier %s and operational policy outcomes", best.driver().tier())
        ));

        // 2. Key Factors: Deterministic Factors, Knowledge Evidence & AI Insights
        List<String> keyFactors = new ArrayList<>();
        keyFactors.add(String.format("Deterministic composite score: %.3f awarded to driver '%s'", bestScore.value(), best.driver().name()));
        keyFactors.add(String.format("Remaining HOS: %d hours (%s needed)", best.driver().remainingHos().toHours(), best.totalRequiredDrivingDuration().toHours() + "h"));
        keyFactors.add(String.format("Vehicle payload capacity: %.0f kg (Shipment: %.0f kg)", best.driver().vehicleWeightCapacityKg(), best.shipment().weightKg()));

        // Knowledge Evidence
        List<GroundingDocument> knowledgeEvidence = context.getFactValue("knowledgeEvidence", List.class)
                .orElse(Collections.emptyList());
        for (GroundingDocument doc : knowledgeEvidence) {
            keyFactors.add(String.format("Knowledge Evidence [%s]: %s (Relevance: %.2f, Source: %s)",
                    doc.documentId(), doc.title(), doc.relevanceScore(), doc.source()));
        }

        if (aiInfluenced) {
            keyFactors.add(String.format("AI Decision Policy: %s", aiInfluenceReason));
        }

        if ("SUCCESS".equalsIgnoreCase(aiStatus)) {
            String aiProviderName = aiTelemetry != null ? aiTelemetry.providerName() : context.getFactValue("aiProviderName", String.class).orElse("SpringAI");
            Double aiConf = aiTelemetry != null && aiTelemetry.advisoryConfidence() != null ? aiTelemetry.advisoryConfidence() : context.getFactValue("aiAdvisoryConfidence", Double.class).orElse(0.90);
            if (best.aiRiskAnalysis() != null && !best.aiRiskAnalysis().isBlank()) {
                keyFactors.add(String.format("AI Context [%s - Advisory Conf: %.0f%%]: %s", aiProviderName, aiConf * 100.0, best.aiRiskAnalysis()));
            }
        } else if ("FALLBACK_TRIGGERED".equalsIgnoreCase(aiStatus)) {
            keyFactors.add("AI Status: Offline / Fallback Active (Deterministic rules sole decider)");
        }

        // 3. Trade-offs
        List<String> tradeOffs = new ArrayList<>();
        if (rankedCandidates.size() > 1) {
            DispatchCandidate runnerUp = rankedCandidates.get(0).equals(best) ? rankedCandidates.get(1) : rankedCandidates.get(0);
            tradeOffs.add(String.format("Alternative candidate '%s' (score: %.3f) had %.1f km deadhead vs %.1f km for '%s'",
                    runnerUp.driver().name(), runnerUp.score().value(), runnerUp.deadheadDistanceKm(), best.deadheadDistanceKm(), best.driver().name()));
        }

        // 4. Recommendation & Explainability
        String rationale = String.format(
                "Assigned driver %s (Deterministic Score: %.3f, Decision Confidence: %.2f) with %.1f km deadhead and estimated delivery at %s.%s",
                best.driver().name(),
                bestScore.value(),
                bestScore.confidence(),
                best.deadheadDistanceKm(),
                best.estimatedDeliveryTime(),
                aiInfluenced ? " Contextual policy favored driver due to corridor risk mitigation." : ""
        );

        Explanation explanation = new Explanation(
                rationale,
                bestScore.confidence(),
                contributions,
                Collections.emptyList(),
                keyFactors,
                tradeOffs
        );

        DispatchAssignment assignment = DispatchAssignment.from(best, rationale);

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("totalCandidatesEvaluated", rankedCandidates.size());
        metadata.put("deadheadDistanceKm", best.deadheadDistanceKm());
        metadata.put("mainDistanceKm", best.mainDistanceKm());
        metadata.put("estimatedCostUsd", best.estimatedTotalCostUsd());
        metadata.put("decisionConfidence", bestScore.confidence());
        metadata.put("aiEnrichmentStatus", aiStatus);
        metadata.put("aiInfluencedDecision", aiInfluenced);
        metadata.put("aiInfluenceReason", aiInfluenceReason);
        metadata.put("initialDeterministicLeader", topCandidate.driver().name());

        KnowledgeTelemetry knowledgeTelemetry = context.getFactValue("knowledgeTelemetry", KnowledgeTelemetry.class).orElse(null);
        if (knowledgeTelemetry != null) {
            metadata.put("knowledgeTelemetry", knowledgeTelemetry);
            metadata.put("knowledgeProvider", knowledgeTelemetry.providerName());
            metadata.put("knowledgeEvidenceCount", knowledgeTelemetry.retrievedCount());
            metadata.put("knowledgeEvidenceIds", knowledgeTelemetry.evidenceDocumentIds());
            metadata.put("knowledgeStatus", knowledgeTelemetry.status());
        }

        if (aiTelemetry != null) {
            metadata.put("aiTelemetry", aiTelemetry);
            if (aiTelemetry.providerName() != null) metadata.put("aiProvider", aiTelemetry.providerName());
            if (aiTelemetry.providerType() != null) metadata.put("aiProviderType", aiTelemetry.providerType());
            if (aiTelemetry.advisoryConfidence() != null) metadata.put("aiAdvisoryConfidence", aiTelemetry.advisoryConfidence());
            metadata.put("aiRiskLevel", aiTelemetry.riskLevel() != null ? aiTelemetry.riskLevel().name() : "LOW");
        }

        Recommendation<DispatchAssignment> recommendation = new Recommendation<>(
                assignment,
                0,
                bestScore,
                bestScore.confidence(),
                rationale,
                explanation,
                metadata
        );

        DecisionContext updatedContext = context
                .withFact(Fact.of("recommendation", recommendation))
                .withFact(Fact.of("explanation", explanation));

        return StepResult.success(
                updatedContext,
                Duration.between(start, Instant.now()),
                List.of(Fact.of("recommendation", recommendation), Fact.of("explanation", explanation)),
                String.format("Assigned %s with composite score %.3f (AI Influenced: %s, Knowledge Grounded: %s)",
                        best.driver().name(), bestScore.value(), aiInfluenced, !knowledgeEvidence.isEmpty())
        );
    }
}
