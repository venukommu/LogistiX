package org.logistix.examples.dispatch.ai;

import org.logistix.ai.dispatch.MockDispatchAIProvider;
import org.logistix.domain.decision.DecisionContext;
import org.logistix.domain.ports.AIProvider;

import java.util.Optional;

/**
 * Legacy reference advisor wrapper delegating to {@link MockDispatchAIProvider}.
 * @deprecated Use {@link org.logistix.ai.dispatch.SpringAIDispatchAIProvider} for live Spring AI integration
 *             or {@link MockDispatchAIProvider} for deterministic testing.
 */
@Deprecated(since = "0.1.0", forRemoval = false)
public class DispatchAIAdvisor implements AIProvider {

    private final MockDispatchAIProvider delegate;

    public DispatchAIAdvisor() {
        this("LogistiX-Deterministic-AI-Advisor", false);
    }

    public DispatchAIAdvisor(String providerName, boolean simulatedOffline) {
        this.delegate = new MockDispatchAIProvider(providerName, simulatedOffline);
    }

    public static DispatchAIAdvisor offline() {
        return new DispatchAIAdvisor("LogistiX-Offline-AI-Advisor", true);
    }

    @Override
    public String getProviderName() {
        return delegate.getProviderName();
    }

    @Override
    public <T> Optional<T> infer(DecisionContext context, Class<T> responseType) {
        return delegate.infer(context, responseType);
    }

    @Override
    public String generateReasoning(DecisionContext context, Object candidateObj) {
        return delegate.generateReasoning(context, candidateObj);
    }
}
