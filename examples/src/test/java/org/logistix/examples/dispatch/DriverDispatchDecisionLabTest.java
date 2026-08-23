package org.logistix.examples.dispatch;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.logistix.ai.dispatch.MockDispatchAIProvider;
import org.logistix.examples.dispatch.lab.DispatchComparisonEngine;
import org.logistix.examples.dispatch.lab.DispatchComparisonInput;
import org.logistix.examples.dispatch.lab.DispatchComparisonResult;
import org.logistix.examples.dispatch.lab.DispatchDecisionMode;
import org.logistix.examples.dispatch.lab.DispatchLabReporter;
import org.logistix.examples.dispatch.lab.DispatchScenario;
import org.logistix.examples.dispatch.lab.DispatchScenarios;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DriverDispatchDecisionLabTest {

    private final Instant now = Instant.now();
    private final DispatchComparisonEngine engine = new DispatchComparisonEngine(new MockDispatchAIProvider());

    @Nested
    @DisplayName("1. Golden Demonstration Scenarios")
    class GoldenScenarioTests {

        @Test
        @DisplayName("Scenario 1 (Baseline Clear): AI confirms deterministic recommendation with zero AI invocations in RULES_ONLY and 1 in HYBRID")
        void testScenario1AiConfirms() {
            DispatchScenario scenario = DispatchScenarios.scenario1AiConfirms(now);
            DispatchComparisonResult result = engine.compare(scenario);

            assertThat(result.rulesOnlyDriver()).isEqualTo("Alex 'Swift' Rivera");
            assertThat(result.hybridDriver()).isEqualTo("Alex 'Swift' Rivera");
            assertThat(result.recommendationChanged()).isFalse();

            assertThat(result.aiInvocations()).isEqualTo(1);
            assertThat(result.aiProviderType()).isEqualTo("MOCK");
            assertThat(result.hardConstraintsSatisfied()).isTrue();
            assertThat(result.fallbackTriggered()).isFalse();

            // Verify Explainability
            assertThat(result.aiInsights()).isNotEmpty();
            assertThat(result.aiAdvisoryConfidence()).isGreaterThan(0.8);
            assertThat(result.hybridConfidence()).isGreaterThan(0.9);
        }

        @Test
        @DisplayName("Scenario 2 (Corridor Weather Risk): AI interprets severe storm conditions and adds contextual risk signals")
        void testScenario2AiAddsContext() {
            DispatchScenario scenario = DispatchScenarios.scenario2AiAddsContext(now);
            DispatchComparisonResult result = engine.compare(scenario);

            assertThat(result.rulesOnlyResult().recommendation().item().isAssigned()).isTrue();
            assertThat(result.hybridResult().recommendation().item().isAssigned()).isTrue();
            assertThat(result.hardConstraintsSatisfied()).isTrue();

            assertThat(result.aiInvocations()).isEqualTo(1);
            assertThat(result.aiInsights().stream().anyMatch(i -> i.contains("BLIZZARD") || i.contains("Weather"))).isTrue();
        }

        @Test
        @DisplayName("Scenario 3 (Safety Guardrail): Infeasible and uncertified drivers are rejected deterministically in both modes")
        void testScenario3SafetyGuardrail() {
            DispatchScenario scenario = DispatchScenarios.scenario3SafetyGuardrail(now);
            DispatchComparisonResult result = engine.compare(scenario);

            // Both modes MUST assign the compliant driver
            assertThat(result.rulesOnlyDriver()).isEqualTo("Alex 'Swift' Rivera (Compliant)");
            assertThat(result.hybridDriver()).isEqualTo("Alex 'Swift' Rivera (Compliant)");

            // Neither mode assigned the uncertified or low-HOS driver
            assertThat(result.rulesOnlyDriver()).doesNotContain("No HazMat").doesNotContain("Low HOS");
            assertThat(result.hybridDriver()).doesNotContain("No HazMat").doesNotContain("Low HOS");
            assertThat(result.hardConstraintsSatisfied()).isTrue();
        }
    }

    @Nested
    @DisplayName("2. Decision Lab Invariants & Same-Input Guarantee")
    class InvariantTests {

        @Test
        @DisplayName("Guaranteed identical inputs: DispatchComparisonInput generates congruent DecisionContext for both modes")
        void testSameInputGuarantee() {
            DispatchScenario scenario = DispatchScenarios.scenario1AiConfirms(now);
            DispatchComparisonInput input = DispatchComparisonInput.from(scenario);

            var ctxRules = input.toDecisionContext(DispatchDecisionMode.RULES_ONLY, "corr-1");
            var ctxHybrid = input.toDecisionContext(DispatchDecisionMode.HYBRID_AI, "corr-2");

            // Fact count and candidate list must be identical
            assertThat(ctxRules.getFactValue("candidates", List.class).orElse(null))
                    .isEqualTo(ctxHybrid.getFactValue("candidates", List.class).orElse(null));
            assertThat(ctxRules.getFactValue("shipment", Object.class).orElse(null))
                    .isEqualTo(ctxHybrid.getFactValue("shipment", Object.class).orElse(null));

            // Environment attributes must match
            assertThat(ctxRules.environment()).isEqualTo(ctxHybrid.environment());
        }

        @Test
        @DisplayName("Reporting formats (Terminal Box & JSON) render correctly without exceptions")
        void testReportingFormats() {
            DispatchScenario scenario = DispatchScenarios.scenario1AiConfirms(now);
            DispatchComparisonResult result = engine.compare(scenario);

            String box = DispatchLabReporter.formatSideBySideBox(result);
            assertThat(box).contains("LOGISTIX DECISION LAB").contains("WITHOUT AI").contains("WITH AI");

            String json = DispatchLabReporter.formatJson(result);
            assertThat(json).contains("\"scenarioId\"")
                    .contains("\"baseline-clear\"")
                    .contains("\"rulesOnly\"")
                    .contains("\"hybrid\"")
                    .contains("\"comparison\"");
        }
    }
}
