package org.logistix.dsl.fluent;

import org.logistix.domain.decision.DecisionContext;
import org.logistix.domain.fact.Fact;
import org.logistix.domain.fact.FactBag;
import org.logistix.domain.fact.FactSource;
import org.logistix.dsl.builder.ContextBuilder;

/**
 * Fluent API for assembling DecisionContext instances.
 */
public class FluentContext {

    private final ContextBuilder builder;

    public FluentContext(String decisionType) {
        this.builder = new ContextBuilder().decisionType(decisionType);
    }

    public static FluentContext of(String decisionType) {
        return new FluentContext(decisionType);
    }

    public <T> FluentContext fact(String key, T value) {
        this.builder.fact(key, value);
        return this;
    }

    public <T> FluentContext fact(String key, T value, FactSource source) {
        this.builder.fact(key, value, source);
        return this;
    }

    public FluentContext fact(Fact<?> fact) {
        this.builder.fact(fact);
        return this;
    }

    public FluentContext facts(FactBag facts) {
        this.builder.facts(facts);
        return this;
    }

    public FluentContext environment(String key, Object value) {
        this.builder.environment(key, value);
        return this;
    }

    public FluentContext parameter(String key, Object value) {
        this.builder.parameter(key, value);
        return this;
    }

    public DecisionContext build() {
        return this.builder.build();
    }
}
