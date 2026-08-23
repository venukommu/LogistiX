package org.logistix.examples.dispatch;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.logistix.ai.dispatch.BatchedDispatchAIAdvice;
import org.logistix.ai.dispatch.DispatchAIAdvice;
import org.logistix.ai.dispatch.MockDispatchAIProvider;
import org.logistix.ai.dispatch.RiskLevel;
import org.logistix.domain.decision.DecisionContext;
import org.logistix.domain.decision.DecisionResult;
import org.logistix.domain.ports.AIProvider;
import org.logistix.domain.ports.KnowledgeProvider;
import org.logistix.domain.ports.KnowledgeProvider.GroundingDocument;
import org.logistix.domain.ports.KnowledgeProvider.KnowledgeQuery;
import org.logistix.dsl.LogistiX;
import org.logistix.engine.executor.DecisionExecutor;
import org.logistix.engine.pipeline.DecisionPipeline;
import org.logistix.examples.dispatch.lab.DispatchComparisonEngine;
import org.logistix.examples.dispatch.lab.DispatchComparisonResult;
import org.logistix.examples.dispatch.lab.DispatchScenario;
import org.logistix.examples.dispatch.lab.DispatchScenarios;
import org.logistix.examples.dispatch.model.DispatchAssignment;
import org.logistix.examples.dispatch.pipeline.DispatchDecisionPipelineFactory;
import org.logistix.rag.knowledge.InMemoryKnowledgeProvider;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test suite verifying Knowledge-Aware Decision Intelligence invariants (Sprint 9).
 */
public class DriverDispatchKnowledgeAwareTest {

    private final Instant now = Instant.now();
    private final InMemoryKnowledgeProvider knowledgeProvider = InMemoryKnowledgeProvider.withDefaults();
    private final MockDispatchAIProvider aiProvider = new MockDispatchAIProvider();
    private final DispatchComparisonEngine engine = new DispatchComparisonEngine(aiProvider, knowledgeProvider);

    @Nested
    @DisplayName("1. Knowledge Provider & Retrieval Invariants")
    class KnowledgeProviderTests {

        @Test
        @DisplayName("TEST 1 & 2: InMemoryKnowledgeProvider returns expected evidence deterministically")
        void testDeterministicRetrieval() {
            KnowledgeQuery query = KnowledgeQuery.of("blizzard donner pass winter chain controls", 2);
            List<GroundingDocument> results1 = knowledgeProvider.retrieveKnowledge(query);
            List<GroundingDocument> results2 = knowledgeProvider.retrieveKnowledge(query);

            assertThat(results1).isNotEmpty();
            assertThat(results1).hasSizeLessThanOrEqualTo(2);
            assertThat(results1.get(0).documentId()).isEqualTo("DOC-WINTER-001");
            assertThat(results1.get(0).title()).contains("Winter Operations");
            assertThat(results1.get(0).relevanceScore()).isGreaterThan(0.5);

            // Determinism check
            assertThat(results1).isEqualTo(results2);
        }

        @Test
        @DisplayName("TEST 3: Top-K limit is strictly respected")
        void testTopKEnforced() {
            KnowledgeQuery query = KnowledgeQuery.of("regulations", 1);
            List<GroundingDocument> results = knowledgeProvider.retrieveKnowledge(query);

            assertThat(results).hasSize(1);
        }

        @Test
        @DisplayName("TEST 10: Knowledge provider failure degrades gracefully with valid decision")
        void testKnowledgeFailureGracefulFallback() {
            DispatchScenario scenario = DispatchScenarios.scenario5KnowledgeAwareDispatch(now);
            DispatchComparisonEngine offlineEngine = new DispatchComparisonEngine(aiProvider, InMemoryKnowledgeProvider.offline());
            DispatchComparisonResult result = offlineEngine.compare(scenario);

            assertThat(result.hardConstraintsSatisfied()).isTrue();
            assertThat(result.safetyStatus()).isEqualTo("SAFE");
            assertThat(result.hybridResult().recommendation().item().isAssigned()).isTrue();
        }

        @Test
        @DisplayName("TEST 11: Empty/No relevant knowledge proceeds safely")
        void testEmptyKnowledgeSafeExecution() {
            DispatchScenario scenario = DispatchScenarios.scenario1AiConfirms(now);
            DispatchComparisonEngine emptyEngine = new DispatchComparisonEngine(aiProvider, InMemoryKnowledgeProvider.empty());
            DispatchComparisonResult result = emptyEngine.compare(scenario);

            assertThat(result.hardConstraintsSatisfied()).isTrue();
            assertThat(result.hybridDriver()).isEqualTo("Alex 'Swift' Rivera");
        }
    }

    @Nested
    @DisplayName("2. Knowledge-Aware Decision Scenarios & Grounding")
    class KnowledgeAwareScenarioTests {

        @Test
        @DisplayName("TEST 12 & 13: Scenario 5 (Knowledge-Aware Dispatch) produces grounded advisory and remains HARD-feasible")
        void testScenario5KnowledgeAwareDispatch() {
            DispatchScenario scenario = DispatchScenarios.scenario5KnowledgeAwareDispatch(now);
            DispatchComparisonResult result = engine.compare(scenario);

            // RULES_ONLY picks Sam based on deadhead
            assertThat(result.rulesOnlyDriver()).isEqualTo("Sam 'Speedy' Miller");

            // KNOWLEDGE_AWARE HYBRID picks Elena (grounded in DOC-WINTER-001)
            assertThat(result.hybridDriver()).isEqualTo("Elena 'Mountain' Rostova");
            assertThat(result.recommendationChanged()).isTrue();
            assertThat(result.aiInfluencedDecision()).isTrue();
            assertThat(result.hardConstraintsSatisfied()).isTrue();
            assertThat(result.knowledgeEvidenceCount()).isGreaterThanOrEqualTo(1);
            assertThat(result.knowledgeEvidenceIds()).contains("DOC-WINTER-001");

            // Verify Explainability distinguishes Knowledge Evidence
            List<String> keyFactors = result.hybridResult().explanation().keyFactors();
            assertThat(keyFactors).anyMatch(k -> k.startsWith("Knowledge Evidence [DOC-WINTER-001]"));
            assertThat(keyFactors).anyMatch(k -> k.contains("AI Context"));
        }

        @Test
        @DisplayName("TEST 4: Phantom/Unknown evidence citations from rogue AI are filtered out")
        void testPhantomEvidenceRejected() {
            DispatchScenario scenario = DispatchScenarios.scenario5KnowledgeAwareDispatch(now);

            AIProvider rogueAi = new AIProvider() {
                @Override public String getProviderName() { return "Rogue-AI"; }
                @Override
                @SuppressWarnings("unchecked")
                public <T> Optional<T> infer(DecisionContext context, Class<T> responseType) {
                    DispatchAIAdvice rogueAdvice = new DispatchAIAdvice(
                            scenario.candidateDrivers().get(1).driverId().toString(),
                            RiskLevel.LOW,
                            0.95,
                            "Grounded in fake document!",
                            List.of(),
                            List.of(),
                            List.of("DOC-FAKE-HALLUCINATED-999"), // PHANTOM CITATION
                            Instant.now()
                    );
                    return Optional.of((T) BatchedDispatchAIAdvice.of(List.of(rogueAdvice), "Rogue Context"));
                }
                @Override public String generateReasoning(DecisionContext context, Object candidate) { return ""; }
            };

            DispatchComparisonEngine rogueEngine = new DispatchComparisonEngine(rogueAi, knowledgeProvider);
            DispatchComparisonResult result = rogueEngine.compare(scenario);

            // Phantom citation is stripped from the explainability narrative
            String analysis = result.hybridResult().recommendation().item().rationale();
            assertThat(analysis).doesNotContain("DOC-FAKE-HALLUCINATED-999");
            assertThat(result.hardConstraintsSatisfied()).isTrue();
        }

        @Test
        @DisplayName("TEST 14: RULES_ONLY performs 0 AI calls and 0 Knowledge calls")
        void testRulesOnlyZeroInvocations() {
            DispatchScenario scenario = DispatchScenarios.scenario5KnowledgeAwareDispatch(now);
            DispatchComparisonResult result = engine.compare(scenario);

            assertThat(result.rulesOnlyResult().recommendation().metadata().get("aiTelemetry")).isNull();
            assertThat(result.rulesOnlyResult().recommendation().metadata().get("knowledgeTelemetry")).isNull();
        }
    }
}
