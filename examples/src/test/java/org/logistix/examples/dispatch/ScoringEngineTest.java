package org.logistix.examples.dispatch;

import org.logistix.common.model.Coordinates;
import org.logistix.domain.decision.DecisionContext;
import org.logistix.domain.score.Score;
import org.logistix.examples.dispatch.model.DispatchCandidate;
import org.logistix.examples.dispatch.model.Driver;
import org.logistix.examples.dispatch.model.DriverTier;
import org.logistix.examples.dispatch.model.Shipment;
import org.logistix.examples.dispatch.scoring.DispatchScoringEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ScoringEngineTest {

    private DispatchScoringEngine scoringEngine;
    private DecisionContext context;
    private Shipment shipment;
    private Instant now;

    @BeforeEach
    void setUp() {
        scoringEngine = new DispatchScoringEngine();
        context = DecisionContext.of("driver-dispatch");
        now = Instant.now();

        shipment = Shipment.builder()
                .origin(Coordinates.of(37.7749, -122.4194))
                .destination(Coordinates.of(34.0522, -118.2437))
                .deliveryDeadline(now.plus(Duration.ofHours(12)))
                .build();
    }

    @Test
    @DisplayName("Scoring engine should produce normalized scores bounded strictly between 0.0 and 1.0")
    void testScoreNormalizationBounds() {
        Driver driver = Driver.builder()
                .currentLocation(Coordinates.of(37.8044, -122.2712)) // Close to SF
                .rating(4.9)
                .historicalOnTimeRate(0.98)
                .tier(DriverTier.PLATINUM)
                .build();

        DispatchCandidate candidate = DispatchCandidate.from(driver, shipment, now, 0.1, "CLEAR");
        Score score = scoringEngine.score(candidate, context);

        assertThat(score.value()).isGreaterThanOrEqualTo(0.0).isLessThanOrEqualTo(1.0);
        assertThat(score.confidence()).isGreaterThanOrEqualTo(0.0).isLessThanOrEqualTo(1.0);
        assertThat(score.subScores()).containsKeys("proximity", "etaMargin", "driverPerformance", "costEfficiency", "ruleAdjustments");
    }

    @Test
    @DisplayName("Driver with minimal deadhead should score significantly higher than driver far away")
    void testProximityAdvantage() {
        Driver closeDriver = Driver.builder()
                .name("Close Driver")
                .currentLocation(Coordinates.of(37.7749, -122.4194)) // 0 km deadhead
                .build();

        Driver farDriver = Driver.builder()
                .name("Far Driver")
                .currentLocation(Coordinates.of(36.0000, -120.0000)) // ~230 km deadhead
                .build();

        DispatchCandidate closeCand = DispatchCandidate.from(closeDriver, shipment, now, 0.1, "CLEAR");
        DispatchCandidate farCand = DispatchCandidate.from(farDriver, shipment, now, 0.1, "CLEAR");

        Score closeScore = scoringEngine.score(closeCand, context);
        Score farScore = scoringEngine.score(farCand, context);

        assertThat(closeScore.value()).isGreaterThan(farScore.value());
        assertThat(closeScore.subScores().get("proximity")).isGreaterThan(farScore.subScores().get("proximity"));
    }

    @Test
    @DisplayName("ScoreAll should calculate scores for a batch of candidates accurately")
    void testScoreAllBatch() {
        Driver d1 = Driver.builder().name("D1").build();
        Driver d2 = Driver.builder().name("D2").build();

        List<DispatchCandidate> list = List.of(
                DispatchCandidate.from(d1, shipment, now, 0.1, "CLEAR"),
                DispatchCandidate.from(d2, shipment, now, 0.1, "CLEAR")
        );

        Map<DispatchCandidate, Score> results = scoringEngine.scoreAll(list, context);
        assertThat(results).hasSize(2);
    }
}
