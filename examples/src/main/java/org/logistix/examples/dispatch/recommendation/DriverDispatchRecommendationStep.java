package org.logistix.examples.dispatch.recommendation;

import org.logistix.domain.decision.DecisionContext;
import org.logistix.domain.explanation.Explanation;
import org.logistix.domain.explanation.FeatureContribution;
import org.logistix.domain.fact.Fact;
import org.logistix.domain.recommendation.Recommendation;
import org.logistix.domain.rule.RuleOutcome;
import org.logistix.domain.score.Score;
import org.logistix.engine.steps.RecommendationStep;
import org.logistix.engine.steps.StepMetadata;
import org.logistix.engine.steps.StepResult;
import org.logistix.examples.dispatch.model.DispatchAssignment;
import org.logistix.examples.dispatch.model.DispatchCandidate;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pipeline step creating the final ranked DispatchAssignment recommendation, complete with
 * human-interpretable feature contribution explanations and trade-off telemetry.
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

            DispatchAssignment unassigned = DispatchAssignment.unassigned(null, "No feasible driver available.");
            Recommendation<DispatchAssignment> emptyRec = new Recommendation<>(
                    unassigned,
                    0,
                    Score.of(0.0, 0.0),
                    0.0,
                    "No feasible driver available.",
                    noCandidatesExp,
                    Collections.emptyMap()
            );

            DecisionContext updatedContext = context
                    .withFact(Fact.of("recommendation", emptyRec))
                    .withFact(Fact.of("explanation", noCandidatesExp));

            return StepResult.success(updatedContext, Duration.between(start, Instant.now()), "No candidates to recommend.");
        }

        DispatchCandidate best = rankedCandidates.get(0);
        Score bestScore = best.score();

        // 1. Build Feature Contributions
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

        // 2. Key Factors
        List<String> keyFactors = new ArrayList<>();
        keyFactors.add(String.format("Driver '%s' achieved the highest composite score of %.3f", best.driver().name(), bestScore.value()));
        keyFactors.add(String.format("Remaining HOS: %d hours (%s needed)", best.driver().remainingHos().toHours(), best.totalRequiredDrivingDuration().toHours() + "h"));
        keyFactors.add(String.format("Vehicle capacity: %.0f kg (Shipment: %.0f kg)", best.driver().vehicleWeightCapacityKg(), best.shipment().weightKg()));
        if (best.aiRiskAnalysis() != null) {
            keyFactors.add("AI Context: " + best.aiRiskAnalysis());
        }

        // 3. Trade-Offs Considered
        List<String> tradeOffs = new ArrayList<>();
        if (rankedCandidates.size() > 1) {
            DispatchCandidate runnerUp = rankedCandidates.get(1);
            tradeOffs.add(String.format("Runner-up '%s' (score: %.3f) had %.1f km deadhead vs %.1f km for '%s'",
                    runnerUp.driver().name(), runnerUp.score().value(), runnerUp.deadheadDistanceKm(), best.deadheadDistanceKm(), best.driver().name()));
        }

        // 4. Synthesize Summary Rationale
        String rationale = String.format("Assigned driver %s (Score: %.3f, Confidence: %.2f) with %.1f km deadhead and estimated delivery at %s.",
                best.driver().name(), bestScore.value(), bestScore.confidence(), best.deadheadDistanceKm(), best.estimatedDeliveryTime());

        Explanation explanation = new Explanation(
                rationale,
                bestScore.confidence(),
                contributions,
                best.ruleOutcomes(),
                keyFactors,
                tradeOffs
        );

        DispatchAssignment assignment = DispatchAssignment.from(best, rationale);

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("totalCandidatesEvaluated", rankedCandidates.size());
        metadata.put("deadheadDistanceKm", best.deadheadDistanceKm());
        metadata.put("mainDistanceKm", best.mainDistanceKm());
        metadata.put("estimatedCostUsd", best.estimatedTotalCostUsd());

        Recommendation<DispatchAssignment> recommendation = new Recommendation<>(
                assignment,
                1,
                bestScore,
                bestScore.confidence(),
                rationale,
                explanation,
                metadata
        );

        Duration duration = Duration.between(start, Instant.now());

        DecisionContext updatedContext = context
                .withFact(Fact.of("recommendation", recommendation))
                .withFact(Fact.of("explanation", explanation))
                .withFact(Fact.of("finalAssignment", assignment));

        return StepResult.success(
                updatedContext,
                duration,
                List.of(Fact.of("recommendation", recommendation)),
                "Recommendation generated successfully for best candidate: " + best.driver().name()
        );
    }
}
