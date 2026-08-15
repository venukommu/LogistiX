package org.logistix.decision.engine;

import org.logistix.decision.explainability.ExplainableRecommendation;

import java.util.List;

/**
 * Generic contract for AI and algorithmic decision engines.
 *
 * @param <C> Input context type
 * @param <R> Output recommendation target type
 */
public interface DecisionEngine<C, R> {

    List<ExplainableRecommendation<R>> evaluate(DecisionContext<C> context);

    ExplainableRecommendation<R> evaluateBest(DecisionContext<C> context);
}
