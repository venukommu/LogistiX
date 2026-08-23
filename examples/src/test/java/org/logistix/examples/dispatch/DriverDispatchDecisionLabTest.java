package org.logistix.examples.dispatch;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.logistix.ai.dispatch.BatchedDispatchAIAdvice;
import org.logistix.ai.dispatch.DispatchAIAdvice;
import org.logistix.ai.dispatch.MockDispatchAIProvider;
import org.logistix.ai.dispatch.RiskLevel;
import org.logistix.domain.decision.DecisionContext;
import org.logistix.domain.ports.AIProvider;
import org.logistix.dsl.LogistiX;
import org.logistix.engine.executor.DecisionExecutor;
import org.logistix.engine.pipeline.DecisionPipeline;
import org.logistix.examples.dispatch.lab.DispatchComparisonEngine;
import org.logistix.examples.dispatch.lab.DispatchComparisonInput;
import org.logistix.examples.dispatch.lab.DispatchComparisonResult;
import org.logistix.examples.dispatch.lab.DispatchDecisionMode;
import org.logistix.examples.dispatch.lab.DispatchLabReporter;
import org.logistix.examples.dispatch.lab.DispatchScenario;
import org.logistix.examples.dispatch.lab.DispatchScenarios;
import org.logistix.examples.dispatch.pipeline.DispatchDecisionPipelineFactory;
import org.logistix.examples.dispatch.simulation.DispatchBenchmarkRunner;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class DriverDispatchDecisionLabTest {

    private final Instant now = Instant.now();
    private final DispatchComparisonEngine engine = new DispatchComparisonEngine(new MockDispatchAIProvider());

    @Nested
    @DisplayName("1. Golden Demonstration Scenarios & Differentiation")
    class GoldenScenarioTests {

        @Test
        @DisplayName("TEST 1 & TEST 4: Baseline Clear - 0 AI calls for rules-only, 1 for hybrid, recommendation unchanged")
        void testScenario1Baseline() {
            DispatchScenario scenario = DispatchScenarios.scenario1AiConfirms(now);
            DispatchComparisonResult result = engine.compare(scenario);

            assertThat(result.rulesOnlyDriver()).isEqualTo("Alex 'Swift' Rivera");
            assertThat(result.hybridDriver()).isEqualTo("Alex 'Swift' Rivera");
            assertThat(result.recommendationChanged()).isFalse();
            assertThat(result.aiInfluencedDecision()).isFalse();

            assertThat(result.aiInvocations()).isEqualTo(1);
            assertThat(result.aiProviderType()).isEqualTo("MOCK");
            assertThat(result.hardConstraintsSatisfied()).isTrue();
            assertThat(result.safetyStatus()).isEqualTo("SAFE");
        }

        @Test
        @DisplayName("TEST 5: AI Contextual Scenario - Recommendation changes (Sam -> Elena) and all HARD constraints remain satisfied")
        void testScenario4AiContextualDecision() {
            DispatchScenario scenario = DispatchScenarios.scenario4AiContextualDecision(now);
            DispatchComparisonResult result = engine.compare(scenario);

            // RULES_ONLY selects Sam 'Speedy' Miller (closer deadhead)
            assertThat(result.rulesOnlyDriver()).isEqualTo("Sam 'Speedy' Miller");

            // HYBRID_AI selects Elena 'Mountain' Rostova (safer winter mountain profile)
            assertThat(result.hybridDriver()).isEqualTo("Elena 'Mountain' Rostova");

            assertThat(result.recommendationChanged()).isTrue();
            assertThat(result.aiInfluencedDecision()).isTrue();
            assertThat(result.aiInfluenceReason()).contains("Elena 'Mountain' Rostova");
            assertThat(result.hardConstraintsSatisfied()).isTrue();
            assertThat(result.safetyStatus()).isEqualTo("SAFE");
        }

        @Test
        @DisplayName("TEST 6: Safety Guardrail Enforcement - Infeasible and uncertified drivers are rejected deterministically in both modes")
        void testScenario3SafetyGuardrail() {
            DispatchScenario scenario = DispatchScenarios.scenario3SafetyGuardrail(now);
            DispatchComparisonResult result = engine.compare(scenario);

            assertThat(result.rulesOnlyDriver()).isEqualTo("Alex 'Swift' Rivera (Compliant)");
            assertThat(result.hybridDriver()).isEqualTo("Alex 'Swift' Rivera (Compliant)");
            assertThat(result.recommendationChanged()).isFalse();
            assertThat(result.hardConstraintsSatisfied()).isTrue();
            assertThat(result.safetyStatus()).isEqualTo("SAFE");
        }
    }

    @Nested
    @DisplayName("2. Invariants, Safety & Anomaly Tests")
    class InvariantAndSafetyTests {

        @Test
        @DisplayName("TEST 3: Same Input Guarantee - rulesOnly input matches hybrid input identically")
        void testSameInputGuarantee() {
            DispatchScenario scenario = DispatchScenarios.scenario4AiContextualDecision(now);
            DispatchComparisonInput input = DispatchComparisonInput.from(scenario);

            var ctxRules = input.toDecisionContext(DispatchDecisionMode.RULES_ONLY, "corr-1");
            var ctxHybrid = input.toDecisionContext(DispatchDecisionMode.HYBRID_AI, "corr-2");

            assertThat(ctxRules.getFactValue("candidates", List.class).orElse(null))
                    .isEqualTo(ctxHybrid.getFactValue("candidates", List.class).orElse(null));
            assertThat(ctxRules.getFactValue("shipment", Object.class).orElse(null))
                    .isEqualTo(ctxHybrid.getFactValue("shipment", Object.class).orElse(null));
            assertThat(ctxRules.environment()).isEqualTo(ctxHybrid.environment());
        }

        @Test
        @DisplayName("TEST 7 & 8: AI Timeout or Faulty Provider triggers graceful fallback")
        void testAiTimeoutFallback() {
            DispatchScenario scenario = DispatchScenarios.scenario1AiConfirms(now);
            DispatchComparisonEngine offlineEngine = new DispatchComparisonEngine(MockDispatchAIProvider.offline());
            DispatchComparisonResult result = offlineEngine.compare(scenario);

            assertThat(result.fallbackTriggered()).isTrue();
            assertThat(result.safetyStatus()).isEqualTo("FALLBACK");
            assertThat(result.hardConstraintsSatisfied()).isTrue();
            assertThat(result.hybridDriver()).isEqualTo("Alex 'Swift' Rivera");
        }

        @Test
        @DisplayName("TEST 9 & 10: Unknown and phantom candidate advice is safely rejected")
        void testUnknownPhantomCandidateRejected() {
            DispatchScenario scenario = DispatchScenarios.scenario1AiConfirms(now);

            AIProvider phantomProvider = new AIProvider() {
                @Override public String getProviderName() { return "Phantom-AI"; }
                @Override
                @SuppressWarnings("unchecked")
                public <T> Optional<T> infer(DecisionContext context, Class<T> responseType) {
                    DispatchAIAdvice fakeAdvice = new DispatchAIAdvice(
                            "phantom-ghost-driver", RiskLevel.LOW, 0.99, "Assign ghost!", List.of(), List.of(), Instant.now()
                    );
                    return Optional.of((T) BatchedDispatchAIAdvice.of(List.of(fakeAdvice), "Context"));
                }
                @Override public String generateReasoning(DecisionContext context, Object candidate) { return ""; }
            };

            DispatchComparisonEngine phantomEngine = new DispatchComparisonEngine(phantomProvider);
            DispatchComparisonResult result = phantomEngine.compare(scenario);

            assertThat(result.hybridDriver()).isEqualTo("Alex 'Swift' Rivera");
            assertThat(result.hardConstraintsSatisfied()).isTrue();
        }

        @Test
        @DisplayName("TEST 11: JSON output contains all required comparison metrics and safety status")
        void testJsonOutputFields() {
            DispatchScenario scenario = DispatchScenarios.scenario4AiContextualDecision(now);
            DispatchComparisonResult result = engine.compare(scenario);

            String json = DispatchLabReporter.formatJson(result);
            assertThat(json)
                    .contains("\"scenarioId\"")
                    .contains("\"ai-contextual-decision\"")
                    .contains("\"recommendationChanged\"")
                    .contains("\"aiInfluencedDecision\"")
                    .contains("\"aiInfluenceReason\"")
                    .contains("\"safetyStatus\"");
        }

        @Test
        @DisplayName("TEST 12: Benchmark accurately reports 0 AI calls for RULES_ONLY and 1 for HYBRID_MOCK")
        void testBenchmarkSemantics() {
            DecisionExecutor executor = LogistiX.getContext().getExecutor();
            DecisionPipeline rulesPipe = DispatchDecisionPipelineFactory.createRulesOnlyPipeline();
            DecisionPipeline hybridPipe = DispatchDecisionPipelineFactory.createHybridAiPipeline();

            var bRules = DispatchBenchmarkRunner.runBenchmark("RULES_ONLY", "JVM", rulesPipe, executor, 5, 4);
            var bHybrid = DispatchBenchmarkRunner.runBenchmark("HYBRID_MOCK", "MOCK", hybridPipe, executor, 5, 4);

            assertThat(bRules.aiCallsPerDecision()).isEqualTo(0);
            assertThat(bRules.benchmarkSemantics()).contains("Deterministic JVM");

            assertThat(bHybrid.aiCallsPerDecision()).isEqualTo(1);
            assertThat(bHybrid.benchmarkSemantics()).contains("In-memory Mock AI");
        }
    }
}
