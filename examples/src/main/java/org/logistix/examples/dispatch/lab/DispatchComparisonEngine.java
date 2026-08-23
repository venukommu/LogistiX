package org.logistix.examples.dispatch.lab;

import org.logistix.ai.dispatch.DispatchAIAdvice;
import org.logistix.ai.dispatch.MockDispatchAIProvider;
import org.logistix.ai.dispatch.RiskLevel;
import org.logistix.domain.decision.DecisionContext;
import org.logistix.domain.decision.DecisionResult;
import org.logistix.domain.ports.AIProvider;
import org.logistix.domain.ports.KnowledgeProvider;
import org.logistix.dsl.LogistiX;
import org.logistix.engine.executor.DecisionExecutor;
import org.logistix.engine.pipeline.DecisionPipeline;
import org.logistix.examples.dispatch.model.DispatchAssignment;
import org.logistix.examples.dispatch.pipeline.DispatchDecisionPipelineFactory;
import org.logistix.rag.knowledge.InMemoryKnowledgeProvider;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Pure orchestration engine executing both RULES_ONLY and KNOWLEDGE_AWARE HYBRID_AI pipelines on identical inputs
 * to measure, compare, and contrast the exact value, grounding, and cost of AI decision augmentation.
 */
public class DispatchComparisonEngine {

    private final AIProvider aiProvider;
    private final KnowledgeProvider knowledgeProvider;
    private final DecisionExecutor executor;

    public DispatchComparisonEngine() {
        this(createDefaultScenarioMockProvider(), new InMemoryKnowledgeProvider());
    }

    public DispatchComparisonEngine(AIProvider aiProvider) {
        this(aiProvider, new InMemoryKnowledgeProvider());
    }

    public DispatchComparisonEngine(AIProvider aiProvider, KnowledgeProvider knowledgeProvider) {
        this.aiProvider = aiProvider != null ? aiProvider : createDefaultScenarioMockProvider();
        this.knowledgeProvider = knowledgeProvider != null ? knowledgeProvider : new InMemoryKnowledgeProvider();
        this.executor = LogistiX.getContext().getExecutor();
    }

    public static MockDispatchAIProvider createDefaultScenarioMockProvider() {
        Instant now = Instant.now();

        return MockDispatchAIProvider.builder()
                .withScenarioAdvice("baseline-clear", List.of(
                        new DispatchAIAdvice("11111111-1111-1111-1111-000000000001", RiskLevel.LOW, 0.92, "Optimal driving corridor between SF and LA.", List.of(), List.of(), List.of(), now),
                        new DispatchAIAdvice("11111111-1111-1111-1111-000000000002", RiskLevel.LOW, 0.88, "Clear route with standard traffic profile.", List.of(), List.of(), List.of(), now)
                ))
                .withScenarioAdvice("corridor-weather-risk", List.of(
                        new DispatchAIAdvice("22222222-2222-2222-2222-000000000001", RiskLevel.LOW, 0.92, "Wet road conditions; verified high safety rating provides reassurance.", List.of("Weather: MODERATE_RAIN"), List.of(), List.of(), now),
                        new DispatchAIAdvice("22222222-2222-2222-2222-000000000002", RiskLevel.MEDIUM, 0.85, "Moderate rain delay risk on Central Valley transit corridor.", List.of("Weather: MODERATE_RAIN"), List.of("Wet road caution advised"), List.of(), now)
                ))
                .withScenarioAdvice("safety-constraint-protection", List.of(
                        new DispatchAIAdvice("33333333-3333-3333-3333-000000000003", RiskLevel.LOW, 0.95, "Fully compliant driver meeting all required certifications.", List.of(), List.of(), List.of(), now)
                ))
                .withScenarioAdvice("ai-contextual-decision", List.of(
                        new DispatchAIAdvice(
                                "44444444-4444-4444-4444-000000000001", // Sam 'Speedy' Miller
                                RiskLevel.HIGH, 0.92,
                                "Standard equipment faces severe chain inspection delays and blizzard vulnerability on mountain pass.",
                                List.of("Weather: BLIZZARD_WARNING_DONNER_PASS"),
                                List.of("High blizzard delay risk on mountain corridor"),
                                List.of(),
                                now
                        ),
                        new DispatchAIAdvice(
                                "44444444-4444-4444-4444-000000000002", // Elena 'Mountain' Rostova
                                RiskLevel.LOW, 0.95,
                                "Platinum winter corridor qualifications and robust safety margin for Donner Pass blizzard.",
                                List.of("Weather: BLIZZARD_WARNING_DONNER_PASS"),
                                List.of(),
                                List.of(),
                                now
                        )
                ))
                .withScenarioAdvice("knowledge-aware-dispatch", List.of(
                        new DispatchAIAdvice(
                                "55555555-5555-5555-5555-000000000001", // Sam 'Speedy' Miller
                                RiskLevel.HIGH, 0.92,
                                "Violates DOC-WINTER-001 winter equipment readiness guidance (chain inspection delay >180m expected).",
                                List.of("Policy: DOC-WINTER-001"),
                                List.of("High blizzard delay risk on mountain corridor"),
                                List.of("DOC-WINTER-001"),
                                now
                        ),
                        new DispatchAIAdvice(
                                "55555555-5555-5555-5555-000000000002", // Elena 'Mountain' Rostova
                                RiskLevel.LOW, 0.95,
                                "Satisfies DOC-WINTER-001 mountain pass readiness with verified winter qualifications and 11h HOS buffer.",
                                List.of("Policy: DOC-WINTER-001"),
                                List.of(),
                                List.of("DOC-WINTER-001"),
                                now
                        )
                ))
                .build();
    }

    /**
     * Executes the comparison for a given scenario.
     */
    public DispatchComparisonResult compare(DispatchScenario scenario) {
        Objects.requireNonNull(scenario, "scenario must not be null");

        DispatchComparisonInput input = DispatchComparisonInput.from(scenario);

        // 1. Execute RULES_ONLY pipeline (Zero AI Calls, Zero Knowledge Calls)
        DecisionPipeline rulesPipeline = DispatchDecisionPipelineFactory.createRulesOnlyPipeline();
        DecisionContext rulesContext = input.toDecisionContext(DispatchDecisionMode.RULES_ONLY, scenario.scenarioId() + "-rules")
                .withParameter("scenarioId", scenario.scenarioId());
        DecisionResult<DispatchAssignment> rulesResult = executor.execute(rulesPipeline, rulesContext);

        // 2. Execute KNOWLEDGE_AWARE HYBRID_AI pipeline (Knowledge Retrieval + Exactly ONE Batched AI Call)
        DecisionPipeline hybridPipeline = DispatchDecisionPipelineFactory.createKnowledgeAwarePipeline(aiProvider, knowledgeProvider);
        DecisionContext hybridContext = input.toDecisionContext(DispatchDecisionMode.HYBRID_AI, scenario.scenarioId() + "-hybrid")
                .withParameter("scenarioId", scenario.scenarioId());
        DecisionResult<DispatchAssignment> hybridResult = executor.execute(hybridPipeline, hybridContext);

        // 3. Assemble and return comparative results
        return DispatchComparisonResult.of(scenario, rulesResult, hybridResult);
    }
}
