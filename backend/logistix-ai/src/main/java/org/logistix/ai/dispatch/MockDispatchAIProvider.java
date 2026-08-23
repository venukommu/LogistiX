package org.logistix.ai.dispatch;

import org.logistix.domain.decision.DecisionContext;
import org.logistix.domain.ports.AIProvider;
import org.logistix.domain.ports.KnowledgeProvider.GroundingDocument;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Deterministic Mock AI Provider for offline testing, CI environments, and simulated failure testing.
 * Accurately models contextual risk differentiation and grounds reasoning in retrieved knowledge evidence.
 */
public class MockDispatchAIProvider implements AIProvider {

    private final String providerName;
    private final boolean simulatedOffline;
    private final AtomicInteger invocationCount = new AtomicInteger(0);

    public MockDispatchAIProvider() {
        this("Mock-Deterministic-Dispatch-AI", false);
    }

    public MockDispatchAIProvider(String providerName, boolean simulatedOffline) {
        this.providerName = providerName != null ? providerName : "Mock-Deterministic-Dispatch-AI";
        this.simulatedOffline = simulatedOffline;
    }

    public static MockDispatchAIProvider offline() {
        return new MockDispatchAIProvider("Mock-Offline-AI", true);
    }

    public int getInvocationCount() {
        return invocationCount.get();
    }

    public void resetInvocationCount() {
        invocationCount.set(0);
    }

    @Override
    public String getProviderName() {
        return providerName;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> infer(DecisionContext context, Class<T> responseType) {
        invocationCount.incrementAndGet();

        if (simulatedOffline) {
            return Optional.empty();
        }

        String weather = context.getEnvironmentAttribute("weatherAdvisory", String.class).orElse("CLEAR");
        DispatchAIRequest request = context.getFactValue("aiRequest", DispatchAIRequest.class).orElse(null);
        List<DispatchAIAdvice> advices = new ArrayList<>();

        List<String> availableEvidenceIds = (request != null && request.knowledgeEvidence() != null)
                ? request.knowledgeEvidence().stream().map(GroundingDocument::documentId).toList()
                : Collections.emptyList();

        if (request != null && !request.candidates().isEmpty()) {
            for (CandidatePromptContext c : request.candidates()) {
                RiskLevel risk;
                String reasoning;
                List<String> warnings = Collections.emptyList();
                List<String> evidenceUsed = new ArrayList<>();

                if (weather.contains("BLIZZARD") || weather.contains("STORM")) {
                    if ("PLATINUM".equalsIgnoreCase(c.driverTier()) || c.driverRating() >= 4.9) {
                        risk = RiskLevel.LOW;
                        if (availableEvidenceIds.contains("DOC-WINTER-001")) {
                            evidenceUsed.add("DOC-WINTER-001");
                            reasoning = String.format("Mock AI Analysis: Driver '%s' satisfies DOC-WINTER-001 mountain pass readiness with verified winter qualifications and 11h HOS buffer for %s.",
                                    c.driverName(), weather);
                        } else {
                            reasoning = String.format("Mock AI Analysis: Driver '%s' has Platinum winter corridor qualifications and robust safety margin for %s.",
                                    c.driverName(), weather);
                        }
                    } else {
                        risk = RiskLevel.HIGH;
                        warnings = List.of("High blizzard delay risk on mountain corridor", "Standard equipment chain restrictions");
                        if (availableEvidenceIds.contains("DOC-WINTER-001")) {
                            evidenceUsed.add("DOC-WINTER-001");
                            reasoning = String.format("Mock AI Analysis: Driver '%s' violates DOC-WINTER-001 guidance (lacks Tier-1 winter equipment; chain inspection delay >180m expected during %s).",
                                    c.driverName(), weather);
                        } else {
                            reasoning = String.format("Mock AI Analysis: Driver '%s' has Standard equipment; high vulnerability to %s delay.",
                                    c.driverName(), weather);
                        }
                    }
                } else if (weather.contains("RAIN")) {
                    risk = RiskLevel.MEDIUM;
                    reasoning = String.format("Mock AI Analysis: Driver '%s' evaluated under wet conditions '%s'.", c.driverName(), weather);
                    warnings = List.of("Wet road caution advised");
                } else {
                    risk = RiskLevel.LOW;
                    reasoning = String.format("Mock AI Analysis: Driver '%s' evaluated under optimal weather condition '%s'.", c.driverName(), weather);
                }

                advices.add(new DispatchAIAdvice(
                        c.candidateId(),
                        risk,
                        0.92,
                        reasoning,
                        List.of("Weather: " + weather, "Tier: " + c.driverTier()),
                        warnings,
                        evidenceUsed,
                        Instant.now()
                ));
            }
        } else {
            RiskLevel fallbackRisk = weather.contains("STORM") || weather.contains("BLIZZARD") ? RiskLevel.HIGH
                    : weather.contains("RAIN") ? RiskLevel.MEDIUM : RiskLevel.LOW;
            advices.add(new DispatchAIAdvice(
                    "mock-candidate-id",
                    fallbackRisk,
                    0.92,
                    "Mock AI reasoning: Evaluated operational corridor context.",
                    List.of("Weather Condition: " + weather),
                    fallbackRisk != RiskLevel.LOW ? List.of("Adverse weather slowdown expected") : Collections.emptyList(),
                    Collections.emptyList(),
                    Instant.now()
            ));
        }

        if (responseType.isAssignableFrom(BatchedDispatchAIAdvice.class)) {
            BatchedDispatchAIAdvice batched = BatchedDispatchAIAdvice.of(advices, "Mock AI evaluated operational corridor.");
            return Optional.of((T) batched);
        } else if (responseType.isAssignableFrom(DispatchAIAdvice.class)) {
            return Optional.of((T) advices.get(0));
        }

        return Optional.empty();
    }

    @Override
    public String generateReasoning(DecisionContext context, Object candidate) {
        invocationCount.incrementAndGet();

        if (simulatedOffline) {
            throw new RuntimeException("Mock AI Provider connection timeout (simulated offline mode)");
        }

        String weather = context.getEnvironmentAttribute("weatherAdvisory", String.class).orElse("CLEAR");
        if (weather.contains("SNOW") || weather.contains("STORM") || weather.contains("BLIZZARD")) {
            return String.format("Mock AI Analysis: Weather advisory active ('%s'). Elevated corridor risk identified.", weather);
        }

        return "Mock AI Analysis: Standard operational conditions. Route and timing optimal.";
    }
}
