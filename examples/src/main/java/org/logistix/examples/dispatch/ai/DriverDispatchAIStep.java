package org.logistix.examples.dispatch.ai;

import org.logistix.ai.dispatch.DispatchAIAdvice;
import org.logistix.domain.decision.DecisionContext;
import org.logistix.domain.fact.Fact;
import org.logistix.domain.ports.AIProvider;
import org.logistix.engine.steps.AIStep;
import org.logistix.engine.steps.StepMetadata;
import org.logistix.engine.steps.StepResult;
import org.logistix.examples.dispatch.model.DispatchCandidate;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Optional pipeline step providing AI/LLM contextual risk assessment, structured advice,
 * narrative reasoning, and trade-off analysis for top candidates with graceful fallback.
 */
public class DriverDispatchAIStep implements AIStep {

    public static final String STEP_ID = "step-dispatch-ai";
    public static final String STEP_NAME = "AI Dispatch Contextual Advisor";

    private final AIProvider aiProvider;

    public DriverDispatchAIStep() {
        this(new DispatchAIAdvisor());
    }

    public DriverDispatchAIStep(AIProvider aiProvider) {
        this.aiProvider = aiProvider;
    }

    @Override
    public StepMetadata getMetadata() {
        // Explicitly declared as optional to guarantee zero pipeline disruption
        return StepMetadata.optional(STEP_ID, STEP_NAME, 40);
    }

    @Override
    @SuppressWarnings("unchecked")
    public StepResult execute(DecisionContext context) {
        Instant start = Instant.now();

        List<DispatchCandidate> rankedCandidates = context.getFactValue("rankedCandidates", List.class)
                .orElse(Collections.emptyList());

        if (rankedCandidates.isEmpty()) {
            return StepResult.skipped(context, "No candidates available for AI reasoning.");
        }

        try {
            List<DispatchCandidate> enrichedCandidates = new ArrayList<>();
            // Analyze top 3 candidates
            int countToAnalyze = Math.min(3, rankedCandidates.size());
            DispatchAIAdvice primaryAdvice = null;

            for (int i = 0; i < rankedCandidates.size(); i++) {
                DispatchCandidate candidate = rankedCandidates.get(i);
                if (i < countToAnalyze) {
                    String reasoning = aiProvider.generateReasoning(context, candidate);
                    enrichedCandidates.add(candidate.withAiRiskAnalysis(reasoning));
                } else {
                    enrichedCandidates.add(candidate);
                }
            }

            // Attempt structured inference for the top candidate
            Optional<DispatchAIAdvice> adviceOpt = aiProvider.infer(context, DispatchAIAdvice.class);
            if (adviceOpt.isPresent()) {
                primaryAdvice = adviceOpt.get();
            }

            Duration duration = Duration.between(start, Instant.now());
            DecisionContext updatedContext = context
                    .withFact(Fact.of("rankedCandidates", enrichedCandidates))
                    .withFact(Fact.of("aiEnrichmentStatus", "SUCCESS"))
                    .withFact(Fact.of("aiProviderName", aiProvider.getProviderName()));

            if (primaryAdvice != null) {
                updatedContext = updatedContext
                        .withFact(Fact.of("aiAdvice", primaryAdvice))
                        .withFact(Fact.of("aiAdvisoryConfidence", primaryAdvice.advisoryConfidence()))
                        .withFact(Fact.of("aiRiskLevel", primaryAdvice.riskLevel().name()));
            }

            return StepResult.success(
                    updatedContext,
                    duration,
                    List.of(Fact.of("rankedCandidates", enrichedCandidates)),
                    String.format("AI Advisor completed reasoning for top %d candidates via %s",
                            countToAnalyze, aiProvider.getProviderName())
            );

        } catch (Exception e) {
            // Graceful degradation: Fallback to deterministic rules without failing the decision!
            Duration duration = Duration.between(start, Instant.now());
            DecisionContext updatedContext = context
                    .withFact(Fact.of("aiEnrichmentStatus", "FALLBACK_TRIGGERED"))
                    .withFact(Fact.of("aiFallbackReason", e.getMessage()))
                    .withFact(Fact.of("aiProviderName", aiProvider.getProviderName()));

            return StepResult.success(
                    updatedContext,
                    duration,
                    String.format("AI step degraded gracefully to deterministic fallback: %s", e.getMessage())
            );
        }
    }
}
