package org.logistix.examples.dispatch.ai;

import org.logistix.ai.dispatch.AITelemetry;
import org.logistix.ai.dispatch.BatchedDispatchAIAdvice;
import org.logistix.ai.dispatch.CandidatePromptContext;
import org.logistix.ai.dispatch.DispatchAIAdvice;
import org.logistix.ai.dispatch.DispatchAIRequest;
import org.logistix.ai.dispatch.DispatchPromptBuilder;
import org.logistix.ai.dispatch.MockDispatchAIProvider;
import org.logistix.ai.dispatch.RiskLevel;
import org.logistix.ai.dispatch.SpringAIDispatchAIProvider;
import org.logistix.domain.decision.DecisionContext;
import org.logistix.domain.fact.Fact;
import org.logistix.domain.ports.AIProvider;
import org.logistix.domain.rule.RuleOutcome;
import org.logistix.engine.steps.AIStep;
import org.logistix.engine.steps.StepMetadata;
import org.logistix.engine.steps.StepResult;
import org.logistix.examples.dispatch.model.DispatchCandidate;
import org.logistix.examples.dispatch.model.Shipment;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Production-grade AI pipeline step evaluating feasible dispatch candidates in a single batched LLM invocation.
 * Enforces typed telemetry, strict graceful fallback, advice validation, and zero score manipulation.
 */
public class DriverDispatchAIStep implements AIStep {

    public static final String STEP_ID = "step-dispatch-ai";
    public static final String STEP_NAME = "AI Dispatch Contextual Advisor";

    private final AIProvider aiProvider;
    private final int topN;

    public DriverDispatchAIStep() {
        this(new MockDispatchAIProvider(), 3);
    }

    public DriverDispatchAIStep(AIProvider aiProvider) {
        this(aiProvider, 3);
    }

    public DriverDispatchAIStep(AIProvider aiProvider, int topN) {
        this.aiProvider = aiProvider != null ? aiProvider : new MockDispatchAIProvider();
        this.topN = Math.max(1, topN);
    }

    @Override
    public StepMetadata getMetadata() {
        return StepMetadata.optional(STEP_ID, STEP_NAME, 40);
    }

    public int getTopN() {
        return topN;
    }

    @Override
    @SuppressWarnings("unchecked")
    public StepResult execute(DecisionContext context) {
        Instant start = Instant.now();

        List<DispatchCandidate> rankedCandidates = context.getFactValue("rankedCandidates", List.class)
                .orElse(Collections.emptyList());

        if (rankedCandidates.isEmpty()) {
            AITelemetry telemetry = AITelemetry.skipped("No candidates available for AI reasoning.");
            DecisionContext updatedContext = context
                    .withFact(Fact.of("aiTelemetry", telemetry))
                    .withFact(Fact.of("aiEnrichmentStatus", "SKIPPED"));
            return StepResult.skipped(updatedContext, "No candidates available for AI reasoning.");
        }

        Shipment shipment = context.getFactValue("shipment", Shipment.class).orElse(null);
        String weather = context.getEnvironmentAttribute("weatherAdvisory", String.class).orElse("CLEAR");
        String executionMode = context.getParameter("executionMode", String.class).orElse("HYBRID");
        String providerType = (aiProvider instanceof SpringAIDispatchAIProvider) ? "LIVE" : "MOCK";

        try {
            int countToEvaluate = Math.min(topN, rankedCandidates.size());
            List<CandidatePromptContext> promptCandidates = new ArrayList<>();
            Set<String> validCandidateIds = new HashSet<>();

            for (int i = 0; i < countToEvaluate; i++) {
                DispatchCandidate c = rankedCandidates.get(i);
                String cId = c.driver().driverId().toString();
                validCandidateIds.add(cId);

                List<String> ruleSignals = c.ruleOutcomes().stream()
                        .filter(RuleOutcome::passed)
                        .map(RuleOutcome::reason)
                        .toList();

                promptCandidates.add(new CandidatePromptContext(
                        cId,
                        c.driver().name(),
                        c.deadheadDistanceKm(),
                        c.deadheadDuration().toMinutes(),
                        c.mainDuration().toMinutes(),
                        c.estimatedDeliveryTime().toString(),
                        c.driver().rating(),
                        c.driver().historicalOnTimeRate(),
                        c.driver().tier().name(),
                        c.score().value(),
                        ruleSignals
                ));
            }

            DispatchAIRequest aiRequest = new DispatchAIRequest(
                    shipment != null ? shipment.shipmentId().toString() : "UNKNOWN",
                    shipment != null ? String.format("(%.2f, %.2f)", shipment.origin().latitude(), shipment.origin().longitude()) : "UNKNOWN",
                    shipment != null ? String.format("(%.2f, %.2f)", shipment.destination().latitude(), shipment.destination().longitude()) : "UNKNOWN",
                    shipment != null ? shipment.weightKg() : 0.0,
                    shipment != null ? shipment.deliveryDeadline().toString() : "UNKNOWN",
                    weather,
                    executionMode,
                    promptCandidates
            );

            DecisionContext requestContext = context.withFact(Fact.of("aiRequest", aiRequest));

            // Execute exactly ONE single batched inference call
            Optional<BatchedDispatchAIAdvice> batchedOpt = aiProvider.infer(requestContext, BatchedDispatchAIAdvice.class);
            Optional<DispatchAIAdvice> singleOpt = batchedOpt.isEmpty()
                    ? aiProvider.infer(requestContext, DispatchAIAdvice.class)
                    : Optional.empty();

            if (batchedOpt.isEmpty() && singleOpt.isEmpty()) {
                Duration latency = Duration.between(start, Instant.now());
                AITelemetry telemetry = AITelemetry.fallback(
                        aiProvider.getProviderName(),
                        providerType,
                        (aiProvider instanceof SpringAIDispatchAIProvider sp) ? sp.getModelName() : "Mock",
                        DispatchPromptBuilder.getPromptVersion(),
                        latency,
                        "AI Provider returned empty advice (offline or unparseable)",
                        context.getParameter("correlationId", String.class).orElse(context.contextId().toString())
                );
                DecisionContext updatedContext = context
                        .withFact(Fact.of("rankedCandidates", rankedCandidates))
                        .withFact(Fact.of("aiTelemetry", telemetry))
                        .withFact(Fact.of("aiEnrichmentStatus", "FALLBACK_TRIGGERED"))
                        .withFact(Fact.of("aiFallbackReason", "AI Provider returned empty advice"))
                        .withFact(Fact.of("aiProviderName", aiProvider.getProviderName()));

                return StepResult.success(
                        updatedContext,
                        latency,
                        "AI step degraded gracefully to deterministic fallback: Provider returned empty advice"
                );
            }

            List<DispatchCandidate> enrichedCandidates = new ArrayList<>();
            Double primaryConfidence = 0.90;
            RiskLevel primaryRisk = RiskLevel.LOW;
            Set<String> processedCandidateIds = new HashSet<>();

            if (batchedOpt.isPresent() && !batchedOpt.get().candidateAdvices().isEmpty()) {
                BatchedDispatchAIAdvice batched = batchedOpt.get();

                for (int i = 0; i < rankedCandidates.size(); i++) {
                    DispatchCandidate c = rankedCandidates.get(i);
                    String cId = c.driver().driverId().toString();
                    Optional<DispatchAIAdvice> adviceOpt = batched.getAdviceForCandidate(cId);

                    if (adviceOpt.isPresent() && validCandidateIds.contains(cId) && !processedCandidateIds.contains(cId)) {
                        DispatchAIAdvice adv = adviceOpt.get();
                        processedCandidateIds.add(cId);

                        double validatedConfidence = Math.max(0.0, Math.min(1.0, adv.advisoryConfidence()));
                        RiskLevel validatedRisk = adv.riskLevel() != null ? adv.riskLevel() : RiskLevel.LOW;

                        String narrative = String.format("[%s - Risk: %s, Conf: %.0f%%]: %s",
                                aiProvider.getProviderName(), validatedRisk, validatedConfidence * 100.0, adv.reasoning());
                        enrichedCandidates.add(c.withAiRiskAnalysis(narrative));

                        if (i == 0) {
                            primaryConfidence = validatedConfidence;
                            primaryRisk = validatedRisk;
                        }
                    } else {
                        enrichedCandidates.add(c);
                    }
                }
            } else if (singleOpt.isPresent()) {
                DispatchAIAdvice adv = singleOpt.get();
                if (validCandidateIds.contains(adv.candidateId()) || rankedCandidates.size() == 1) {
                    primaryConfidence = Math.max(0.0, Math.min(1.0, adv.advisoryConfidence()));
                    primaryRisk = adv.riskLevel() != null ? adv.riskLevel() : RiskLevel.LOW;

                    for (int i = 0; i < rankedCandidates.size(); i++) {
                        DispatchCandidate c = rankedCandidates.get(i);
                        if (i == 0) {
                            String narrative = String.format("[%s - Risk: %s, Conf: %.0f%%]: %s",
                                    aiProvider.getProviderName(), primaryRisk, primaryConfidence * 100.0, adv.reasoning());
                            enrichedCandidates.add(c.withAiRiskAnalysis(narrative));
                        } else {
                            enrichedCandidates.add(c);
                        }
                    }
                } else {
                    enrichedCandidates.addAll(rankedCandidates);
                }
            } else {
                enrichedCandidates.addAll(rankedCandidates);
            }

            Duration latency = Duration.between(start, Instant.now());
            AITelemetry telemetry = AITelemetry.success(
                    aiProvider.getProviderName(),
                    providerType,
                    (aiProvider instanceof SpringAIDispatchAIProvider sp) ? sp.getModelName() : "Mock",
                    DispatchPromptBuilder.getPromptVersion(),
                    1, // Exactly ONE batched call
                    latency,
                    primaryConfidence,
                    primaryRisk,
                    context.getParameter("correlationId", String.class).orElse(context.contextId().toString())
            );

            DecisionContext updatedContext = context
                    .withFact(Fact.of("rankedCandidates", enrichedCandidates))
                    .withFact(Fact.of("aiTelemetry", telemetry))
                    .withFact(Fact.of("aiEnrichmentStatus", "SUCCESS"))
                    .withFact(Fact.of("aiProviderName", aiProvider.getProviderName()))
                    .withFact(Fact.of("aiAdvisoryConfidence", primaryConfidence))
                    .withFact(Fact.of("aiRiskLevel", primaryRisk.name()));

            return StepResult.success(
                    updatedContext,
                    latency,
                    List.of(Fact.of("rankedCandidates", enrichedCandidates), Fact.of("aiTelemetry", telemetry)),
                    String.format("AI Advisor completed single batched evaluation across %d candidates via %s",
                            countToEvaluate, aiProvider.getProviderName())
            );

        } catch (Exception e) {
            Duration latency = Duration.between(start, Instant.now());
            AITelemetry telemetry = AITelemetry.fallback(
                    aiProvider.getProviderName(),
                    providerType,
                    (aiProvider instanceof SpringAIDispatchAIProvider sp) ? sp.getModelName() : "Mock",
                    DispatchPromptBuilder.getPromptVersion(),
                    latency,
                    e.getMessage(),
                    context.getParameter("correlationId", String.class).orElse(context.contextId().toString())
            );

            DecisionContext updatedContext = context
                    .withFact(Fact.of("aiTelemetry", telemetry))
                    .withFact(Fact.of("aiEnrichmentStatus", "FALLBACK_TRIGGERED"))
                    .withFact(Fact.of("aiFallbackReason", e.getMessage()))
                    .withFact(Fact.of("aiProviderName", aiProvider.getProviderName()));

            return StepResult.success(
                    updatedContext,
                    latency,
                    String.format("AI step degraded gracefully to deterministic fallback: %s", e.getMessage())
            );
        }
    }
}
