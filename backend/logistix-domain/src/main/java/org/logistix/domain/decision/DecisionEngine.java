package org.logistix.domain.decision;

import java.util.List;

/**
 * Primary decision intelligence interface orchestrating the decision flow:
 * DecisionContext &rarr; ConstraintEngine &rarr; RuleEngine &rarr; AI Provider &rarr; ScoringEngine &rarr; RecommendationEngine &rarr; DecisionResult.
 *
 * @param <C> Candidate type
 * @param <R> Recommendation result target type
 */
public interface DecisionEngine<C, R> {

    String getEngineId();

    String getSupportedDecisionType();

    DecisionResult<R> decide(DecisionContext context, List<C> candidates);

    DecisionResponse<R> decideRanked(DecisionRequest<C> request);
}
