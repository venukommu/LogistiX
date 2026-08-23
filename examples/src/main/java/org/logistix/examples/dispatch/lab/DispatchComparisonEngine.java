package org.logistix.examples.dispatch.lab;

import org.logistix.ai.dispatch.MockDispatchAIProvider;
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
        this(new MockDispatchAIProvider(), new InMemoryKnowledgeProvider());
    }

    public DispatchComparisonEngine(AIProvider aiProvider) {
        this(aiProvider, new InMemoryKnowledgeProvider());
    }

    public DispatchComparisonEngine(AIProvider aiProvider, KnowledgeProvider knowledgeProvider) {
        this.aiProvider = aiProvider != null ? aiProvider : new MockDispatchAIProvider();
        this.knowledgeProvider = knowledgeProvider != null ? knowledgeProvider : new InMemoryKnowledgeProvider();
        this.executor = LogistiX.getContext().getExecutor();
    }

    /**
     * Executes the comparison for a given scenario.
     */
    public DispatchComparisonResult compare(DispatchScenario scenario) {
        Objects.requireNonNull(scenario, "scenario must not be null");

        DispatchComparisonInput input = DispatchComparisonInput.from(scenario);

        // 1. Execute RULES_ONLY pipeline (Zero AI Calls, Zero Knowledge Calls)
        DecisionPipeline rulesPipeline = DispatchDecisionPipelineFactory.createRulesOnlyPipeline();
        DecisionContext rulesContext = input.toDecisionContext(DispatchDecisionMode.RULES_ONLY, scenario.scenarioId() + "-rules");
        DecisionResult<DispatchAssignment> rulesResult = executor.execute(rulesPipeline, rulesContext);

        // 2. Execute KNOWLEDGE_AWARE HYBRID_AI pipeline (Knowledge Retrieval + Exactly ONE Batched AI Call)
        DecisionPipeline hybridPipeline = DispatchDecisionPipelineFactory.createKnowledgeAwarePipeline(aiProvider, knowledgeProvider);
        DecisionContext hybridContext = input.toDecisionContext(DispatchDecisionMode.HYBRID_AI, scenario.scenarioId() + "-hybrid");
        DecisionResult<DispatchAssignment> hybridResult = executor.execute(hybridPipeline, hybridContext);

        // 3. Assemble and return comparative results
        return DispatchComparisonResult.of(scenario, rulesResult, hybridResult);
    }
}
