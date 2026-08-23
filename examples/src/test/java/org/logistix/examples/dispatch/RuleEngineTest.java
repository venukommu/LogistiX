package org.logistix.examples.dispatch;

import org.logistix.common.model.Coordinates;
import org.logistix.domain.decision.DecisionContext;
import org.logistix.domain.rule.RuleOutcome;
import org.logistix.examples.dispatch.model.DispatchCandidate;
import org.logistix.examples.dispatch.model.Driver;
import org.logistix.examples.dispatch.model.DriverTier;
import org.logistix.examples.dispatch.model.Shipment;
import org.logistix.examples.dispatch.rules.PreferredDriverRule;
import org.logistix.examples.dispatch.rules.RegionalAffinityRule;
import org.logistix.examples.dispatch.rules.RestBalanceRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class RuleEngineTest {

    private DecisionContext context;
    private Shipment shipment;
    private Instant now;

    @BeforeEach
    void setUp() {
        context = DecisionContext.of("driver-dispatch");
        now = Instant.now();
        shipment = Shipment.builder()
                .destinationRegion("US-WEST")
                .build();
    }

    @Test
    @DisplayName("PreferredDriverRule should assign tiered score bonuses")
    void testPreferredDriverRule() {
        PreferredDriverRule rule = new PreferredDriverRule();

        Driver platDriver = Driver.builder().tier(DriverTier.PLATINUM).build();
        Driver goldDriver = Driver.builder().tier(DriverTier.GOLD).build();
        Driver stdDriver = Driver.builder().tier(DriverTier.STANDARD).build();

        RuleOutcome platOutcome = rule.evaluate(DispatchCandidate.from(platDriver, shipment, now, 0.1, "CLEAR"), context);
        RuleOutcome goldOutcome = rule.evaluate(DispatchCandidate.from(goldDriver, shipment, now, 0.1, "CLEAR"), context);
        RuleOutcome stdOutcome = rule.evaluate(DispatchCandidate.from(stdDriver, shipment, now, 0.1, "CLEAR"), context);

        assertThat(platOutcome.scoreAdjustment()).isEqualTo(0.15);
        assertThat(goldOutcome.scoreAdjustment()).isEqualTo(0.10);
        assertThat(stdOutcome.scoreAdjustment()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("RestBalanceRule should penalize drivers within 90 minutes of mandatory rest")
    void testRestBalanceRule() {
        RestBalanceRule rule = new RestBalanceRule();

        Driver fatiguedDriver = Driver.builder().timeUntilMandatoryRest(Duration.ofMinutes(45)).build();
        Driver freshDriver = Driver.builder().timeUntilMandatoryRest(Duration.ofHours(5)).build();

        RuleOutcome outcomeFatigued = rule.evaluate(DispatchCandidate.from(fatiguedDriver, shipment, now, 0.1, "CLEAR"), context);
        RuleOutcome outcomeFresh = rule.evaluate(DispatchCandidate.from(freshDriver, shipment, now, 0.1, "CLEAR"), context);

        assertThat(outcomeFatigued.passed()).isFalse();
        assertThat(outcomeFatigued.scoreAdjustment()).isEqualTo(-0.10);

        assertThat(outcomeFresh.passed()).isTrue();
        assertThat(outcomeFresh.scoreAdjustment()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("RegionalAffinityRule should grant bonus when destination matches driver home region")
    void testRegionalAffinityRule() {
        RegionalAffinityRule rule = new RegionalAffinityRule();

        Driver localDriver = Driver.builder().homeRegion("US-WEST").build();
        Driver outOfRegionDriver = Driver.builder().homeRegion("US-EAST").build();

        RuleOutcome outcomeLocal = rule.evaluate(DispatchCandidate.from(localDriver, shipment, now, 0.1, "CLEAR"), context);
        RuleOutcome outcomeOut = rule.evaluate(DispatchCandidate.from(outOfRegionDriver, shipment, now, 0.1, "CLEAR"), context);

        assertThat(outcomeLocal.scoreAdjustment()).isEqualTo(0.08);
        assertThat(outcomeOut.scoreAdjustment()).isEqualTo(0.0);
    }
}
