package org.logistix.dsl.builder;

import org.logistix.domain.decision.DecisionContext;
import org.logistix.domain.fact.Fact;
import org.logistix.domain.fact.FactBag;
import org.logistix.domain.fact.FactSource;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Fluent builder for creating immutable DecisionContext instances.
 */
public class ContextBuilder {

    private UUID contextId = UUID.randomUUID();
    private String decisionType = "generic-decision";
    private final List<Fact<?>> facts = new ArrayList<>();
    private final Map<String, Object> environment = new LinkedHashMap<>();
    private final Map<String, Object> parameters = new LinkedHashMap<>();
    private Instant timestamp = Instant.now();

    public ContextBuilder contextId(UUID contextId) {
        this.contextId = Objects.requireNonNull(contextId, "Context ID cannot be null");
        return this;
    }

    public ContextBuilder decisionType(String decisionType) {
        this.decisionType = Objects.requireNonNull(decisionType, "Decision type cannot be null");
        return this;
    }

    public <T> ContextBuilder fact(String key, T value) {
        this.facts.add(Fact.of(key, value));
        return this;
    }

    public <T> ContextBuilder fact(String key, T value, FactSource source) {
        this.facts.add(Fact.of(key, value, source));
        return this;
    }

    public ContextBuilder fact(Fact<?> fact) {
        this.facts.add(Objects.requireNonNull(fact, "Fact cannot be null"));
        return this;
    }

    public ContextBuilder facts(FactBag factBag) {
        if (factBag != null) {
            this.facts.addAll(factBag.all());
        }
        return this;
    }

    public ContextBuilder environment(String key, Object value) {
        this.environment.put(key, value);
        return this;
    }

    public ContextBuilder parameter(String key, Object value) {
        this.parameters.put(key, value);
        return this;
    }

    public DecisionContext build() {
        return new DecisionContext(
                contextId,
                decisionType,
                FactBag.of(facts),
                environment,
                parameters,
                timestamp
        );
    }
}
