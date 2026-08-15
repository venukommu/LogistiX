package org.logistix.domain.recommendation;

import org.logistix.domain.explanation.Explanation;
import org.logistix.domain.score.Score;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Generic framework container encapsulating any recommended candidate, choice, or operational assignment.
 * Not tied to dispatching; represents any decision recommendation.
 *
 * @param <T> Candidate recommendation type (e.g. Driver, Carrier, Route, PricingTier, DockBay, RiskScore)
 */
public record Recommendation<T>(
        T item,
        int rank,
        Score score,
        double confidence,
        String rationale,
        Explanation explanation,
        Map<String, Object> metadata
) {
    public Recommendation {
        Objects.requireNonNull(item, "Recommended item must not be null");
        Objects.requireNonNull(score, "Score must not be null");
        Objects.requireNonNull(rationale, "Rationale must not be null");
        metadata = metadata != null ? Map.copyOf(metadata) : Collections.emptyMap();
    }

    public static <T> Recommendation<T> of(T item, int rank, Score score, String rationale) {
        return new Recommendation<>(item, rank, score, score.confidence(), rationale, Explanation.simple(rationale, score.confidence()), Collections.emptyMap());
    }

    public static <T> Recommendation<T> of(T item, int rank, Score score, String rationale, Explanation explanation) {
        return new Recommendation<>(item, rank, score, score.confidence(), rationale, explanation, Collections.emptyMap());
    }
}
