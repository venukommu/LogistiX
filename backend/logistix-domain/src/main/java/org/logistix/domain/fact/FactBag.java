package org.logistix.domain.fact;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Immutable container for domain facts indexed by string keys.
 * Enables arbitrary, domain-agnostic fact injection into DecisionContext.
 */
public record FactBag(Map<String, Fact<?>> facts) {

    public FactBag {
        facts = facts != null ? Collections.unmodifiableMap(new LinkedHashMap<>(facts)) : Collections.emptyMap();
    }

    public static FactBag empty() {
        return new FactBag(Collections.emptyMap());
    }

    public static FactBag of(Collection<Fact<?>> factList) {
        if (factList == null || factList.isEmpty()) {
            return empty();
        }
        Map<String, Fact<?>> map = new LinkedHashMap<>();
        for (Fact<?> fact : factList) {
            map.put(fact.key(), fact);
        }
        return new FactBag(map);
    }

    public static FactBag of(Fact<?>... factArray) {
        if (factArray == null || factArray.length == 0) {
            return empty();
        }
        Map<String, Fact<?>> map = new LinkedHashMap<>();
        for (Fact<?> fact : factArray) {
            map.put(fact.key(), fact);
        }
        return new FactBag(map);
    }

    public boolean contains(String key) {
        return facts.containsKey(key);
    }

    public Optional<Fact<?>> get(String key) {
        return Optional.ofNullable(facts.get(key));
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<T> getValue(String key, Class<T> expectedType) {
        Fact<?> fact = facts.get(key);
        if (fact == null || fact.value() == null) {
            return Optional.empty();
        }
        if (expectedType.isInstance(fact.value())) {
            return Optional.of((T) fact.value());
        }
        return Optional.empty();
    }

    public FactBag with(Fact<?> fact) {
        Map<String, Fact<?>> newFacts = new LinkedHashMap<>(this.facts);
        newFacts.put(fact.key(), fact);
        return new FactBag(newFacts);
    }

    public FactBag withAll(Collection<Fact<?>> additionalFacts) {
        if (additionalFacts == null || additionalFacts.isEmpty()) {
            return this;
        }
        Map<String, Fact<?>> newFacts = new LinkedHashMap<>(this.facts);
        for (Fact<?> fact : additionalFacts) {
            newFacts.put(fact.key(), fact);
        }
        return new FactBag(newFacts);
    }

    public Collection<Fact<?>> all() {
        return facts.values();
    }

    public int size() {
        return facts.size();
    }

    public boolean isEmpty() {
        return facts.isEmpty();
    }
}
