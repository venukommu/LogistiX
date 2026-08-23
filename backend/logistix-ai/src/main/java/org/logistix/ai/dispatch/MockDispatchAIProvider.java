package org.logistix.ai.dispatch;

import org.logistix.domain.decision.DecisionContext;
import org.logistix.domain.ports.AIProvider;
import org.logistix.domain.ports.KnowledgeProvider.GroundingDocument;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Deterministic, configurable Mock AI Provider for offline testing, CI environments, and simulated failure testing.
 * Acts purely as a test double without embedding knowledge-specific business rules or document interpretation.
 */
public class MockDispatchAIProvider implements AIProvider {

    private final String providerName;
    private final boolean simulatedOffline;
    private final Map<String, DispatchAIAdvice> configuredCandidateAdvices;
    private final AtomicInteger invocationCount = new AtomicInteger(0);

    public MockDispatchAIProvider() {
        this("Mock-Deterministic-Dispatch-AI", false, Collections.emptyMap());
    }

    public MockDispatchAIProvider(String providerName, boolean simulatedOffline) {
        this(providerName, simulatedOffline, Collections.emptyMap());
    }

    public MockDispatchAIProvider(String providerName, boolean simulatedOffline, Map<String, DispatchAIAdvice> configuredAdvices) {
        this.providerName = providerName != null ? providerName : "Mock-Deterministic-Dispatch-AI";
        this.simulatedOffline = simulatedOffline;
        this.configuredCandidateAdvices = configuredAdvices != null ? new LinkedHashMap<>(configuredAdvices) : new LinkedHashMap<>();
    }

    public static MockDispatchAIProvider offline() {
        return new MockDispatchAIProvider("Mock-Offline-AI", true);
    }

    public static Builder builder() {
        return new Builder();
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
                // Check if specific advice was configured for this candidate
                if (configuredCandidateAdvices.containsKey(c.candidateId())) {
                    advices.add(configuredCandidateAdvices.get(c.candidateId()));
                    continue;
                }

                // Generic, document-agnostic test double evaluation
                RiskLevel risk;
                String reasoning;
                List<String> warnings = Collections.emptyList();
                List<String> evidenceUsed = new ArrayList<>(availableEvidenceIds);

                if (weather.contains("BLIZZARD") || weather.contains("STORM")) {
                    if ("PLATINUM".equalsIgnoreCase(c.driverTier()) || c.driverRating() >= 4.9) {
                        risk = RiskLevel.LOW;
                        reasoning = String.format("Mock AI Analysis: Driver '%s' has top-tier rating and suitable operational qualifications for %s.",
                                c.driverName(), weather);
                    } else {
                        risk = RiskLevel.HIGH;
                        warnings = List.of("High weather delay risk on active transit corridor", "Equipment slowdown expected");
                        reasoning = String.format("Mock AI Analysis: Driver '%s' has standard qualifications; elevated risk during %s.",
                                c.driverName(), weather);
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
                    availableEvidenceIds,
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

    public static class Builder {
        private String providerName = "Mock-Deterministic-Dispatch-AI";
        private boolean simulatedOffline = false;
        private final Map<String, DispatchAIAdvice> candidateAdvices = new LinkedHashMap<>();

        public Builder providerName(String providerName) {
            this.providerName = providerName;
            return this;
        }

        public Builder simulatedOffline(boolean simulatedOffline) {
            this.simulatedOffline = simulatedOffline;
            return this;
        }

        public Builder withCandidateAdvice(DispatchAIAdvice advice) {
            Objects.requireNonNull(advice, "advice must not be null");
            this.candidateAdvices.put(advice.candidateId(), advice);
            return this;
        }

        public Builder withCandidateAdvice(String candidateId, RiskLevel risk, double confidence, String reasoning, List<String> evidenceUsed) {
            this.candidateAdvices.put(candidateId, new DispatchAIAdvice(
                    candidateId,
                    risk,
                    confidence,
                    reasoning,
                    List.of(),
                    List.of(),
                    evidenceUsed != null ? evidenceUsed : List.of(),
                    Instant.now()
            ));
            return this;
        }

        public MockDispatchAIProvider build() {
            return new MockDispatchAIProvider(providerName, simulatedOffline, candidateAdvices);
        }
    }
}
