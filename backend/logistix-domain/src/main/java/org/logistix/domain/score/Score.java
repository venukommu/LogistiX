package org.logistix.domain.score;

import java.util.Collections;
import java.util.Map;

/**
 * Immutable multi-dimensional score awarded to a candidate during evaluation.
 */
public record Score(
        double value,
        double confidence,
        Map<String, Double> subScores
) {
    public Score {
        if (value < 0.0 || value > 1.0) {
            throw new IllegalArgumentException("Normalized score value must be between 0.0 and 1.0");
        }
        if (confidence < 0.0 || confidence > 1.0) {
            throw new IllegalArgumentException("Confidence must be between 0.0 and 1.0");
        }
        subScores = subScores != null ? Map.copyOf(subScores) : Collections.emptyMap();
    }

    public static Score of(double value, double confidence) {
        return new Score(value, confidence, Collections.emptyMap());
    }

    public static Score of(double value, double confidence, Map<String, Double> subScores) {
        return new Score(value, confidence, subScores);
    }
}
