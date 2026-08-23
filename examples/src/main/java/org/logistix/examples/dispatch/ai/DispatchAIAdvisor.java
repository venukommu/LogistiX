package org.logistix.examples.dispatch.ai;

import org.logistix.domain.decision.DecisionContext;
import org.logistix.domain.ports.AIProvider;
import org.logistix.examples.dispatch.model.DispatchCandidate;

import java.util.Optional;

/**
 * AI Advisor analyzing operational risks, weather disruptions, and trade-offs for candidate assignments.
 */
public class DispatchAIAdvisor implements AIProvider {

    private final String providerName;
    private final boolean simulatedOffline;

    public DispatchAIAdvisor() {
        this("LogistiX-Dispatch-LLM-Advisor", false);
    }

    public DispatchAIAdvisor(String providerName, boolean simulatedOffline) {
        this.providerName = providerName;
        this.simulatedOffline = simulatedOffline;
    }

    @Override
    public String getProviderName() {
        return providerName;
    }

    @Override
    public <T> Optional<T> infer(DecisionContext context, Class<T> responseType) {
        if (simulatedOffline) {
            return Optional.empty();
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
        } else {
            reasoning.append("Optimal route conditions observed. ");
        }

        // Deadhead and margin summary
        reasoning.append(String.format("Minimal deadhead distance of %.1f km ensures fuel efficiency. Scheduled arrival provides robust buffer for customer SLA.",
                candidate.deadheadDistanceKm()));

        return reasoning.toString();
    }
}
