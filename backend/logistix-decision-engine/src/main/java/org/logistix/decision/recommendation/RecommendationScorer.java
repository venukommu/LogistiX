package org.logistix.decision.recommendation;

import org.logistix.decision.engine.DecisionContext;

import java.util.List;

/**
 * Strategy interface for multi-criteria candidate scoring.
 *
 * @param <T> Candidate entity type
 */
public interface RecommendationScorer<T> {

    CandidateScoring<T> score(T candidate, DecisionContext<?> context);

    List<CandidateScoring<T>> scoreAll(List<T> candidates, DecisionContext<?> context);
}
