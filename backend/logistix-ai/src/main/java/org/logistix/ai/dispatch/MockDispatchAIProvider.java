package org.logistix.ai.dispatch;

import org.logistix.domain.decision.DecisionContext;
import org.logistix.domain.ports.AIProvider;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Deterministic Mock AI Provider for offline testing, CI environments, and simulated failure testing.
 */
public class MockDispatchAIProvider implements AIProvider {

    private final String providerName;
    private final boolean simulatedOffline;

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

    @Override
    public String getProviderName() {
        return providerName;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> infer(DecisionContext context, Class<T> responseType) {
        if (simulatedOffline) {
            return Optional.empty();
        }

        if (responseType.isAssignableFrom(DispatchAIAdvice.class)) {
            String weather = context.getEnvironmentAttribute("weatherAdvisory", String.class).orElse("CLEAR");
            RiskLevel risk = weather.contains("STORM") || weather.contains("BLIZZARD") ? RiskLevel.HIGH : RiskLevel.LOW;
            double adjustment = (risk == RiskLevel.HIGH) ? -0.05 : 0.05;

            DispatchAIAdvice advice = new DispatchAIAdvice(
                    "mock-candidate-id",
                    risk,
                    0.92,
                    "Mock AI reasoning: Evaluated operational weather and driver historical safety profile.",
                    List.of("Weather Condition: " + weather, "Driver Experience"),
                    (risk == RiskLevel.HIGH) ? List.of("Adverse weather slowdown expected") : List.of(),
                    adjustment,
                    Instant.now()
            );

            return Optional.of((T) advice);
        }

        return Optional.empty();
    }

    @Override
    public String generateReasoning(DecisionContext context, Object candidate) {
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
