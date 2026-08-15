package org.logistix.decision.recommendation;

import java.util.Collections;
import java.util.Map;

/**
 * Score components for a candidate evaluation.
 *
 * @param <T> Candidate entity type
 */
public record CandidateScoring<T>(
        T candidate,
        double totalScore,
        Map<String, Double> subScores
) {
    public CandidateScoring {
        subScores = subScores != null ? Map.copyOf(subScores) : Collections.emptyMap();
    }
}
