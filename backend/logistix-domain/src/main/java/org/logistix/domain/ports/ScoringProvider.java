package org.logistix.domain.ports;

import org.logistix.domain.decision.DecisionContext;
import org.logistix.domain.score.Score;

import java.util.List;
import java.util.Map;

/**
 * Outbound SPI for calculating candidate scores via external models, heuristics, or scoring engines.
 */
public interface ScoringProvider {

    <T> Score calculateScore(T candidate, DecisionContext context);

    <T> Map<T, Score> calculateScores(List<T> candidates, DecisionContext context);
}
