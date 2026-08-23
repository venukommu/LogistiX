package org.logistix.ai.dispatch;

import org.logistix.domain.decision.DecisionContext;
import org.logistix.domain.ports.AIProvider;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Deterministic Mock AI Provider for offline testing, CI environments, and simulated failure testing.
 * Tracks invocation counts for assertion verification.
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
        RiskLevel risk = weather.contains("STORM") || weather.contains("BLIZZARD") ? RiskLevel.HIGH
                : weather.contains("RAIN") ? RiskLevel.MEDIUM : RiskLevel.LOW;

        DispatchAIRequest request = context.getFactValue("aiRequest", DispatchAIRequest.class).orElse(null);
        List<DispatchAIAdvice> advices = new ArrayList<>();

        if (request != null && !request.candidates().isEmpty()) {
            for (CandidatePromptContext c : request.candidates()) {
                advices.add(new DispatchAIAdvice(
                        c.candidateId(),
                        risk,
                        0.92,
                        String.format("Mock AI Analysis: Driver '%s' evaluated under weather condition '%s'.", c.driverName(), weather),
                        List.of("Weather: " + weather, "Tier: " + c.driverTier()),
                        risk != RiskLevel.LOW ? List.of("Weather precaution advised") : Collections.emptyList(),
                        0.0,
                        Instant.now()
                ));
            }
        } else {
            advices.add(new DispatchAIAdvice(
                    "mock-candidate-id",
                    risk,
                    0.92,
                    "Mock AI reasoning: Evaluated operational weather and driver safety profile.",
                    List.of("Weather Condition: " + weather),
                    risk != RiskLevel.LOW ? List.of("Adverse weather slowdown expected") : Collections.emptyList(),
                    0.0,
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
        if (weather.contains("SNOW") || weather.contains("STORM") || weather.contains("RAIN")) {
            return String.format("Mock AI Analysis: Weather advisory active ('%s'). Driver evaluated with elevated caution.", weather);
        }

        return "Mock AI Analysis: Standard operational conditions. Route and timing optimal.";
    }
}
