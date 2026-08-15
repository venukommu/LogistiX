package org.logistix.domain.decision;

/**
 * Pluggable strategy abstraction for deciding which pipeline algorithms, rules, or AI heuristics to invoke.
 *
 * @param <C> Candidate type
 * @param <R> Recommendation result type
 */
public interface DecisionStrategy<C, R> {

    String getStrategyName();

    DecisionResponse<R> execute(DecisionRequest<C> request);
}
