package org.logistix.domain.decision;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Standard framework response containing primary decision results and ranked alternatives.
 *
 * @param <T> Candidate recommendation type
 */
public record DecisionResponse<T>(
        DecisionResult<T> primaryResult,
        List<DecisionResult<T>> alternativeResults,
        DecisionMetadata metadata
) {
    public DecisionResponse {
        Objects.requireNonNull(primaryResult, "Primary result must not be null");
        alternativeResults = alternativeResults != null ? List.copyOf(alternativeResults) : Collections.emptyList();
        metadata = metadata != null ? metadata : primaryResult.metadata();
    }

    public static <T> DecisionResponse<T> of(DecisionResult<T> primaryResult) {
        return new DecisionResponse<>(primaryResult, Collections.emptyList(), primaryResult.metadata());
    }

    public static <T> DecisionResponse<T> of(DecisionResult<T> primaryResult, List<DecisionResult<T>> alternatives) {
        return new DecisionResponse<>(primaryResult, alternatives, primaryResult.metadata());
    }
}
