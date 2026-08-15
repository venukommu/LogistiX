package org.logistix.domain.decision;

import org.logistix.domain.fact.Fact;
import org.logistix.domain.fact.FactBag;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * The Central Heart of the LogistiX Framework.
 *
 * An extensible, immutable context object through which all domain facts, environment state,
 * dynamic parameters, and constraints flow into the decision pipeline.
 *
 * Completely decoupled from specific operational verticals (dispatching, carrier selection,
 * pricing, routing, dock scheduling, fraud detection).
 */
public record DecisionContext(
        UUID contextId,
        String decisionType,
        FactBag facts,
        Map<String, Object> environment,
        Map<String, Object> parameters,
        Instant timestamp
) {
    public DecisionContext {
        Objects.requireNonNull(contextId, "Context ID must not be null");
        Objects.requireNonNull(decisionType, "Decision type must not be null");
        facts = facts != null ? facts : FactBag.empty();
        environment = environment != null ? Collections.unmodifiableMap(new LinkedHashMap<>(environment)) : Collections.emptyMap();
        parameters = parameters != null ? Collections.unmodifiableMap(new LinkedHashMap<>(parameters)) : Collections.emptyMap();
        timestamp = timestamp != null ? timestamp : Instant.now();
    }

    public static DecisionContext of(String decisionType) {
        return new DecisionContext(UUID.randomUUID(), decisionType, FactBag.empty(), Collections.emptyMap(), Collections.emptyMap(), Instant.now());
    }

    public static DecisionContext of(String decisionType, FactBag facts) {
        return new DecisionContext(UUID.randomUUID(), decisionType, facts, Collections.emptyMap(), Collections.emptyMap(), Instant.now());
    }

    public static DecisionContext of(String decisionType, FactBag facts, Map<String, Object> environment, Map<String, Object> parameters) {
        return new DecisionContext(UUID.randomUUID(), decisionType, facts, environment, parameters, Instant.now());
    }

    public <T> Optional<T> getFactValue(String key, Class<T> type) {
        return facts.getValue(key, type);
    }

    public Optional<Fact<?>> getFact(String key) {
        return facts.get(key);
    }

    public boolean hasFact(String key) {
        return facts.contains(key);
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<T> getEnvironmentAttribute(String key, Class<T> type) {
        Object val = environment.get(key);
        if (val != null && type.isInstance(val)) {
            return Optional.of((T) val);
        }
        return Optional.empty();
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<T> getParameter(String key, Class<T> type) {
        Object val = parameters.get(key);
        if (val != null && type.isInstance(val)) {
            return Optional.of((T) val);
        }
        return Optional.empty();
    }

    public DecisionContext withFact(Fact<?> fact) {
        return new DecisionContext(this.contextId, this.decisionType, this.facts.with(fact), this.environment, this.parameters, this.timestamp);
    }

    public DecisionContext withFacts(Collection<Fact<?>> additionalFacts) {
        return new DecisionContext(this.contextId, this.decisionType, this.facts.withAll(additionalFacts), this.environment, this.parameters, this.timestamp);
    }

    public DecisionContext withEnvironmentAttribute(String key, Object value) {
        Map<String, Object> newEnv = new LinkedHashMap<>(this.environment);
        newEnv.put(key, value);
        return new DecisionContext(this.contextId, this.decisionType, this.facts, newEnv, this.parameters, this.timestamp);
    }

    public DecisionContext withParameter(String key, Object value) {
        Map<String, Object> newParams = new LinkedHashMap<>(this.parameters);
        newParams.put(key, value);
        return new DecisionContext(this.contextId, this.decisionType, this.facts, this.environment, newParams, this.timestamp);
    }
}
