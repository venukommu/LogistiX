package org.logistix.domain.recommendation;

import org.logistix.domain.decision.DecisionContext;

import java.util.List;
import java.util.Optional;

/**
 * Engine contract for synthesizing constraint, rule, and scoring outputs into ranked recommendations.
 *
 * @param <C> Candidate type
 */
public interface RecommendationEngine<C> {

    Optional<Recommendation<C>> recommendBest(List<C> candidates, DecisionContext context);

    List<Recommendation<C>> recommendRanked(List<C> candidates, DecisionContext context, int maxResults);
}
