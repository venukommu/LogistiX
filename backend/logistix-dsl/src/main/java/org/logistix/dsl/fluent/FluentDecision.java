package org.logistix.dsl.fluent;

import org.logistix.domain.constraint.Constraint;
import org.logistix.domain.decision.DecisionContext;
import org.logistix.domain.decision.DecisionResult;
import org.logistix.domain.fact.Fact;
import org.logistix.domain.fact.FactBag;
import org.logistix.domain.fact.FactSource;
import org.logistix.domain.rule.Rule;
import org.logistix.dsl.LogistiX;
import org.logistix.engine.executor.DecisionExecutor;
import org.logistix.engine.pipeline.DecisionPipeline;
import org.logistix.engine.steps.DecisionStep;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Fluent builder DSL for assembling and executing an on-the-fly decision workflow.
 *
 * <pre>{@code
 * DecisionResult<Driver> result = LogistiX.decision("driver-dispatch")
 *     .fact("shipment", shipment)
 *     .fact("drivers", candidateDrivers)
 *     .constraint(maxDistanceConstraint)
 *     .rule(seniorityRule)
 *     .execute();
 * }</pre>
 *
 * @param <T> Expected recommendation payload type
 */
public class FluentDecision<T> {

    private final String decisionType;
    private final List<Fact<?>> facts = new ArrayList<>();
    private final Map<String, Object> environment = new LinkedHashMap<>();
    private final Map<String, Object> parameters = new LinkedHashMap<>();
    private final List<Constraint<?>> inlineConstraints = new ArrayList<>();
    private final List<Rule<?>> inlineRules = new ArrayList<>();
    private final List<DecisionStep> inlineSteps = new ArrayList<>();
    private DecisionExecutor customExecutor;

    public FluentDecision(String decisionType) {
        this.decisionType = Objects.requireNonNull(decisionType, "Decision type cannot be null");
    }

    public <V> FluentDecision<T> fact(String key, V value) {
        this.facts.add(Fact.of(key, value));
        return this;
    }

    public <V> FluentDecision<T> fact(String key, V value, FactSource source) {
        this.facts.add(Fact.of(key, value, source));
        return this;
    }

    public FluentDecision<T> fact(Fact<?> fact) {
        this.facts.add(Objects.requireNonNull(fact, "Fact cannot be null"));
        return this;
    }

    public FluentDecision<T> facts(FactBag factBag) {
        if (factBag != null) {
            this.facts.addAll(factBag.all());
        }
        return this;
    }

    public FluentDecision<T> environment(String key, Object value) {
        this.environment.put(key, value);
        return this;
    }

    public FluentDecision<T> parameter(String key, Object value) {
        this.parameters.put(key, value);
        return this;
    }

    public FluentDecision<T> constraint(Constraint<?> constraint) {
        this.inlineConstraints.add(Objects.requireNonNull(constraint, "Constraint cannot be null"));
        return this;
    }

    public FluentDecision<T> rule(Rule<?> rule) {
        this.inlineRules.add(Objects.requireNonNull(rule, "Rule cannot be null"));
        return this;
    }

    public FluentDecision<T> step(DecisionStep step) {
        this.inlineSteps.add(Objects.requireNonNull(step, "Step cannot be null"));
        return this;
    }

    public FluentDecision<T> usingExecutor(DecisionExecutor executor) {
        this.customExecutor = executor;
        return this;
    }

    public DecisionContext toContext() {
        return new DecisionContext(
                UUID.randomUUID(),
                this.decisionType,
                FactBag.of(this.facts),
                this.environment,
                this.parameters,
                java.time.Instant.now()
        );
    }

    public DecisionResult<T> execute() {
        DecisionContext context = toContext();
        DecisionExecutor executor = (this.customExecutor != null)
                ? this.customExecutor
                : LogistiX.getContext().getExecutor();

        if (this.inlineSteps.isEmpty()) {
            return executor.execute(this.decisionType, context);
        } else {
            DecisionPipeline pipeline = DecisionPipeline.builder(this.decisionType)
                    .steps(this.inlineSteps)
                    .build();
            return executor.execute(pipeline, context);
        }
    }
}
