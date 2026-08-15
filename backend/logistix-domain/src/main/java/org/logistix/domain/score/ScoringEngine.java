package org.logistix.domain.score;

import org.logistix.domain.decision.DecisionContext;

import java.util.List;
import java.util.Map;

/**
 * Engine contract for multi-criteria weighting and candidate scoring.
 *
 * @param <T> Candidate type
 */
public interface ScoringEngine<T> {

    Score score(T candidate, DecisionContext context);

    Map<T, Score> scoreAll(List<T> candidates, DecisionContext context);
}
