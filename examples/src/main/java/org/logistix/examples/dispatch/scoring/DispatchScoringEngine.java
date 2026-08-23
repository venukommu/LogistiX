package org.logistix.examples.dispatch.scoring;

import org.logistix.domain.decision.DecisionContext;
import org.logistix.domain.rule.RuleOutcome;
import org.logistix.domain.score.Score;
import org.logistix.domain.score.ScoringEngine;
import org.logistix.examples.dispatch.model.DispatchCandidate;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Multi-criteria scoring engine balancing deadhead distance, ETA buffer, driver reliability,
 * operational cost efficiency, and rule adjustments.
 */
public class DispatchScoringEngine implements ScoringEngine<DispatchCandidate> {

    // Weight configuration (sums to 1.00)
    private final double weightProximity = 0.25;
    private final double weightEtaMargin = 0.25;
    private final double weightDriverPerformance = 0.20;
    private final double weightCostEfficiency = 0.15;
    private final double weightRules = 0.15;

    @Override
    public Score score(DispatchCandidate candidate, DecisionContext context) {
        // 1. Proximity / Deadhead score (0.0 to 1.0)
        double maxDeadheadKm = 250.0;
        double proximityScore = Math.max(0.0, 1.0 - (candidate.deadheadDistanceKm() / maxDeadheadKm));

        // 2. ETA Margin score (buffer relative to delivery deadline)
        long marginSeconds = Duration.between(candidate.estimatedDeliveryTime(), candidate.shipment().deliveryDeadline()).getSeconds();
        long optimalBufferSeconds = 7200; // 2 hours buffer is optimal
        double etaMarginScore = Math.min(1.0, Math.max(0.0, (double) marginSeconds / optimalBufferSeconds));

        // 3. Driver Performance score (rating + historical on-time)
        double ratingNorm = candidate.driver().rating() / 5.0;
        double onTimeNorm = candidate.driver().historicalOnTimeRate();
        double driverPerformanceScore = (ratingNorm * 0.5) + (onTimeNorm * 0.5);

        // 4. Cost Efficiency score
        double costBenchmark = 1500.0;
        double costEfficiencyScore = Math.max(0.0, 1.0 - (candidate.estimatedTotalCostUsd() / costBenchmark));

        // 5. Rule adjustments score (neutral is 0.5, bonus/penalties adjust)
        double totalRuleAdjustment = 0.0;
        for (RuleOutcome outcome : candidate.ruleOutcomes()) {
            totalRuleAdjustment += outcome.scoreAdjustment();
        }
        double ruleScore = Math.min(1.0, Math.max(0.0, 0.50 + totalRuleAdjustment));

        // Weighted Composite Score
        double composite = (proximityScore * weightProximity)
                + (etaMarginScore * weightEtaMargin)
                + (driverPerformanceScore * weightDriverPerformance)
                + (costEfficiencyScore * weightCostEfficiency)
                + (ruleScore * weightRules);

        // Clamp between 0.0 and 1.0
        composite = Math.min(1.0, Math.max(0.0, composite));

        // Confidence calculation based on data completeness
        double confidence = 0.95;

        Map<String, Double> subScores = new LinkedHashMap<>();
        subScores.put("proximity", proximityScore);
        subScores.put("etaMargin", etaMarginScore);
        subScores.put("driverPerformance", driverPerformanceScore);
        subScores.put("costEfficiency", costEfficiencyScore);
        subScores.put("ruleAdjustments", ruleScore);

        return Score.of(composite, confidence, subScores);
    }

    @Override
    public Map<DispatchCandidate, Score> scoreAll(List<DispatchCandidate> candidates, DecisionContext context) {
        Map<DispatchCandidate, Score> map = new LinkedHashMap<>();
        for (DispatchCandidate candidate : candidates) {
            map.put(candidate, score(candidate, context));
        }
        return map;
    }
}
