package org.logistix.ai.dispatch;

import org.logistix.domain.decision.DecisionContext;
import org.logistix.domain.ports.AIProvider;

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
 * Pure, deterministic, configurable Mock AI Provider for offline testing, CI environments, and simulated failure testing.
 * Strictly acts as a test double without implementing domain decision rules, weather analysis, driver evaluation,
 * or knowledge document interpretation.
 */
public class MockDispatchAIProvider implements AIProvider {

    private final String providerName;
    private final boolean simulatedOffline;
    private final Map<String, DispatchAIAdvice> configuredCandidateAdvices;
    private final Map<String, List<DispatchAIAdvice>> configuredScenarioAdvices;
    private final DispatchAIAdvice defaultAdvice;
    private final AtomicInteger invocationCount = new AtomicInteger(0);

    public MockDispatchAIProvider() {
        this("Mock-Deterministic-Dispatch-AI", false, Collections.emptyMap(), defaultScenarioAdvices(), null);
    }

    public MockDispatchAIProvider(String providerName, boolean simulatedOffline) {
        this(providerName, simulatedOffline, Collections.emptyMap(), defaultScenarioAdvices(), null);
    }

    public MockDispatchAIProvider(
            String providerName,
            boolean simulatedOffline,
            Map<String, DispatchAIAdvice> configuredCandidateAdvices,
            Map<String, List<DispatchAIAdvice>> configuredScenarioAdvices,
            DispatchAIAdvice defaultAdvice
    ) {
        this.providerName = providerName != null ? providerName : "Mock-Deterministic-Dispatch-AI";
        this.simulatedOffline = simulatedOffline;
        this.configuredCandidateAdvices = configuredCandidateAdvices != null ? new LinkedHashMap<>(configuredCandidateAdvices) : new LinkedHashMap<>();
        this.configuredScenarioAdvices = configuredScenarioAdvices != null ? new LinkedHashMap<>(configuredScenarioAdvices) : new LinkedHashMap<>();
        this.defaultAdvice = defaultAdvice != null ? defaultAdvice : DispatchAIAdvice.neutral("default-candidate", "Neutral mock contextual advisory.");
    }

    public static MockDispatchAIProvider offline() {
        return new MockDispatchAIProvider("Mock-Offline-AI", true, Collections.emptyMap(), Collections.emptyMap(), null);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Map<String, List<DispatchAIAdvice>> defaultScenarioAdvices() {
        Instant now = Instant.now();
        Map<String, List<DispatchAIAdvice>> map = new LinkedHashMap<>();

        map.put("baseline-clear", List.of(
                new DispatchAIAdvice("11111111-1111-1111-1111-000000000001", RiskLevel.LOW, 0.92, "Optimal driving corridor between SF and LA.", List.of(), List.of(), List.of(), now),
                new DispatchAIAdvice("11111111-1111-1111-1111-000000000002", RiskLevel.LOW, 0.88, "Clear route with standard traffic profile.", List.of(), List.of(), List.of(), now)
        ));

        map.put("corridor-weather-risk", List.of(
                new DispatchAIAdvice("22222222-2222-2222-2222-000000000001", RiskLevel.LOW, 0.92, "Wet road conditions; verified high safety rating provides reassurance.", List.of("Weather: MODERATE_RAIN"), List.of(), List.of(), now),
                new DispatchAIAdvice("22222222-2222-2222-2222-000000000002", RiskLevel.MEDIUM, 0.85, "Moderate rain delay risk on Central Valley transit corridor.", List.of("Weather: MODERATE_RAIN"), List.of("Wet road caution advised"), List.of(), now)
        ));

        map.put("safety-constraint-protection", List.of(
                new DispatchAIAdvice("33333333-3333-3333-3333-000000000003", RiskLevel.LOW, 0.95, "Fully compliant driver meeting all required certifications.", List.of(), List.of(), List.of(), now)
        ));

        map.put("ai-contextual-decision", List.of(
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
        ));

        map.put("knowledge-aware-dispatch", List.of(
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
        ));

        return map;
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

        DispatchAIRequest request = context.getFactValue("aiRequest", DispatchAIRequest.class).orElse(null);
        String scenarioId = context.getParameter("scenarioId", String.class).orElse("");
        List<DispatchAIAdvice> advices = new ArrayList<>();

        // 1. Check if an explicit scenario advice list is configured
        if (!scenarioId.isEmpty() && configuredScenarioAdvices.containsKey(scenarioId)) {
            advices.addAll(configuredScenarioAdvices.get(scenarioId));
        } else if (request != null && !request.candidates().isEmpty()) {
            // 2. Iterate candidates: return configured advice if present, otherwise safe neutral default
            for (CandidatePromptContext c : request.candidates()) {
                if (configuredCandidateAdvices.containsKey(c.candidateId())) {
                    advices.add(configuredCandidateAdvices.get(c.candidateId()));
                } else {
                    // Safe, neutral, document/weather-agnostic default test double response
                    advices.add(new DispatchAIAdvice(
                            c.candidateId(),
                            defaultAdvice.riskLevel(),
                            defaultAdvice.advisoryConfidence(),
                            defaultAdvice.reasoning(),
                            defaultAdvice.contributingFactors(),
                            defaultAdvice.warnings(),
                            defaultAdvice.knowledgeEvidenceUsed(),
                            Instant.now()
                    ));
                }
            }
        } else {
            // Fallback candidate context
            advices.add(new DispatchAIAdvice(
                    "mock-candidate-id",
                    defaultAdvice.riskLevel(),
                    defaultAdvice.advisoryConfidence(),
                    defaultAdvice.reasoning(),
                    defaultAdvice.contributingFactors(),
                    defaultAdvice.warnings(),
                    defaultAdvice.knowledgeEvidenceUsed(),
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

        return "Mock AI Analysis: Contextual advisory evaluation complete.";
    }

    public static class Builder {
        private String providerName = "Mock-Deterministic-Dispatch-AI";
        private boolean simulatedOffline = false;
        private final Map<String, DispatchAIAdvice> candidateAdvices = new LinkedHashMap<>();
        private final Map<String, List<DispatchAIAdvice>> scenarioAdvices = new LinkedHashMap<>();
        private DispatchAIAdvice defaultAdvice = DispatchAIAdvice.neutral("default-candidate", "Neutral mock contextual advisory.");

        public Builder providerName(String providerName) {
            this.providerName = providerName;
            return this;
        }

        public Builder simulatedOffline(boolean simulatedOffline) {
            this.simulatedOffline = simulatedOffline;
            return this;
        }

        public Builder defaultAdvice(DispatchAIAdvice defaultAdvice) {
            this.defaultAdvice = defaultAdvice;
            return this;
        }

        public Builder withCandidateAdvice(DispatchAIAdvice advice) {
            Objects.requireNonNull(advice, "advice must not be null");
            this.candidateAdvices.put(advice.candidateId(), advice);
            return this;
        }

        public Builder withCandidateAdvice(String candidateId, DispatchAIAdvice advice) {
            Objects.requireNonNull(candidateId, "candidateId must not be null");
            Objects.requireNonNull(advice, "advice must not be null");
            this.candidateAdvices.put(candidateId, advice);
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

        public Builder withScenarioAdvice(String scenarioId, List<DispatchAIAdvice> advices) {
            Objects.requireNonNull(scenarioId, "scenarioId must not be null");
            this.scenarioAdvices.put(scenarioId, advices != null ? List.copyOf(advices) : List.of());
            return this;
        }

        public MockDispatchAIProvider build() {
            return new MockDispatchAIProvider(providerName, simulatedOffline, candidateAdvices, scenarioAdvices, defaultAdvice);
        }
    }
}
