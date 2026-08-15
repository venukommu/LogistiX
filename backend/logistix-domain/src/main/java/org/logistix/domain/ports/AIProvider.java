package org.logistix.domain.ports;

import org.logistix.domain.decision.DecisionContext;

import java.util.Optional;

/**
 * Outbound SPI for invoking AI/LLM models for operational reasoning and unstructured analysis.
 */
public interface AIProvider {

    String getProviderName();

    <T> Optional<T> infer(DecisionContext context, Class<T> responseType);

    String generateReasoning(DecisionContext context, Object candidate);
}
