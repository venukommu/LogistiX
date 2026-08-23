package org.logistix.examples.dispatch.ai;

import org.logistix.ai.dispatch.DispatchAIAdvice;
import org.logistix.ai.dispatch.RiskLevel;
import org.logistix.domain.decision.DecisionContext;
import org.logistix.domain.ports.AIProvider;
import org.logistix.examples.dispatch.model.DispatchCandidate;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Deterministic Reference AI Advisor analyzing operational risks, weather disruptions,
 * and trade-offs for candidate assignments during offline/local demonstrations.
 */
public class DispatchAIAdvisor implements AIProvider {

    private final String providerName;
    private final boolean simulatedOffline;

    public DispatchAIAdvisor() {
        this("LogistiX-Deterministic-AI-Advisor", false);
    }

    public DispatchAIAdvisor(String providerName, boolean simulatedOffline) {
        this.providerName = providerName != null ? providerName : "LogistiX-Deterministic-AI-Advisor";
        this.simulatedOffline = simulatedOffline;
    }

    public static DispatchAIAdvisor offline() {
        return new DispatchAIAdvisor("LogistiX-Offline-AI-Advisor", true);
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
            RiskLevel risk = (weather.contains("SNOW") || weather.contains("STORM") || weather.contains("RAIN"))
                    ? RiskLevel.MEDIUM : RiskLevel.LOW;

            DispatchAIAdvice advice = new DispatchAIAdvice(
                    "reference-candidate-id",
                    risk,
                    0.92,
                    generateReasoning(context, null),
                    List.of("Weather Condition: " + weather, "Fleet Operational Proximity"),
                    risk != RiskLevel.LOW ? List.of("Weather risk detected on transit corridor") : List.of(),
                    0.0,
                    Instant.now()
            );
            return Optional.of((T) advice);
        }

        return Optional.empty();
    }

    @Override
    public String generateReasoning(DecisionContext context, Object candidateObj) {
        if (simulatedOffline) {
            throw new RuntimeException("AI Provider connection timeout (simulated offline mode)");
        }

        if (!(candidateObj instanceof DispatchCandidate candidate)) {
            return "AI Analysis: Candidate evaluated successfully.";
        }

        StringBuilder reasoning = new StringBuilder();
        reasoning.append("AI Risk & Contextual Analysis: ");

        // Weather check
        String weather = context.getEnvironmentAttribute("weatherAdvisory", String.class).orElse("CLEAR");
        if (weather.contains("SNOW") || weather.contains("STORM")) {
            reasoning.append(String.format("Adverse weather condition detected ('%s'). Driver '%s' recommended due to high on-time reliability (%.0f%%). ",
                    weather, candidate.driver().name(), candidate.driver().historicalOnTimeRate() * 100.0));
        } else if (weather.contains("RAIN")) {
            reasoning.append(String.format("Wet road condition ('%s'). Driver '%s' evaluated with safety margin. ",
                    weather, candidate.driver().name()));
        } else {
            reasoning.append("Optimal route conditions observed. ");
        }

        // Deadhead and margin summary
        reasoning.append(String.format("Minimal deadhead distance of %.1f km ensures fuel efficiency. Scheduled arrival provides robust buffer for customer SLA.",
                candidate.deadheadDistanceKm()));

        return reasoning.toString();
    }
}
