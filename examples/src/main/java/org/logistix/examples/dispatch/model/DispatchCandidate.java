package org.logistix.examples.dispatch.model;

import org.logistix.common.model.Coordinates;
import org.logistix.domain.rule.RuleOutcome;
import org.logistix.domain.score.Score;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Evaluated pairing of a candidate Driver for a specific Shipment, with precomputed trip kinematics.
 */
public record DispatchCandidate(
        Driver driver,
        Shipment shipment,
        double deadheadDistanceKm,
        Duration deadheadDuration,
        double mainDistanceKm,
        Duration mainDuration,
        Instant estimatedPickupTime,
        Instant estimatedDeliveryTime,
        double estimatedTotalCostUsd,
        Score score,
        List<RuleOutcome> ruleOutcomes,
        String aiRiskAnalysis
) {
    public DispatchCandidate {
        Objects.requireNonNull(driver, "Driver must not be null");
        Objects.requireNonNull(shipment, "Shipment must not be null");
        Objects.requireNonNull(deadheadDuration, "Deadhead duration must not be null");
        Objects.requireNonNull(mainDuration, "Main duration must not be null");
        Objects.requireNonNull(estimatedPickupTime, "Estimated pickup time must not be null");
        Objects.requireNonNull(estimatedDeliveryTime, "Estimated delivery time must not be null");
        ruleOutcomes = ruleOutcomes != null ? List.copyOf(ruleOutcomes) : Collections.emptyList();
    }

    public static DispatchCandidate from(Driver driver, Shipment shipment, Instant now, double trafficIndex, String weather) {
        DispatchRoute deadheadRoute = DispatchRoute.estimate(driver.currentLocation(), shipment.origin(), trafficIndex, weather);
        DispatchRoute mainRoute = DispatchRoute.estimate(shipment.origin(), shipment.destination(), trafficIndex, weather);

        Instant pickup = now.plus(deadheadRoute.estimatedTransitTime());
        // If driver arrives before pickup window starts, they wait until pickup window start
        if (pickup.isBefore(shipment.pickupTimeWindowStart())) {
            pickup = shipment.pickupTimeWindowStart();
        }
        Instant delivery = pickup.plus(mainRoute.estimatedTransitTime());

        double totalDistKm = deadheadRoute.distanceKm() + mainRoute.distanceKm();
        // Estimated operating cost: $1.20 per km fuel/wear + tolls
        double operatingCost = (totalDistKm * 1.20) + deadheadRoute.tollCostUsd() + mainRoute.tollCostUsd();

        return new DispatchCandidate(
                driver,
                shipment,
                deadheadRoute.distanceKm(),
                deadheadRoute.estimatedTransitTime(),
                mainRoute.distanceKm(),
                mainRoute.estimatedTransitTime(),
                pickup,
                delivery,
                operatingCost,
                Score.of(0.0, 1.0),
                Collections.emptyList(),
                null
        );
    }

    public Duration totalRequiredDrivingDuration() {
        return deadheadDuration.plus(mainDuration);
    }

    public DispatchCandidate withScore(Score newScore) {
        return new DispatchCandidate(driver, shipment, deadheadDistanceKm, deadheadDuration, mainDistanceKm,
                mainDuration, estimatedPickupTime, estimatedDeliveryTime, estimatedTotalCostUsd, newScore, ruleOutcomes, aiRiskAnalysis);
    }

    public DispatchCandidate withRuleOutcomes(List<RuleOutcome> outcomes) {
        return new DispatchCandidate(driver, shipment, deadheadDistanceKm, deadheadDuration, mainDistanceKm,
                mainDuration, estimatedPickupTime, estimatedDeliveryTime, estimatedTotalCostUsd, score, outcomes, aiRiskAnalysis);
    }

    public DispatchCandidate withAiRiskAnalysis(String analysis) {
        return new DispatchCandidate(driver, shipment, deadheadDistanceKm, deadheadDuration, mainDistanceKm,
                mainDuration, estimatedPickupTime, estimatedDeliveryTime, estimatedTotalCostUsd, score, ruleOutcomes, analysis);
    }
}
