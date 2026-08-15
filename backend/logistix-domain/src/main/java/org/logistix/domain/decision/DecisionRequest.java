package org.logistix.domain.decision;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Immutable request payload initiating an operational decision workflow.
 *
 * @param <T> Candidate evaluation type
 */
public record DecisionRequest<T>(
        DecisionContext context,
        List<T> candidates,
        DecisionMetadata metadata
) {
    public DecisionRequest {
        Objects.requireNonNull(context, "Context must not be null");
        candidates = candidates != null ? List.copyOf(candidates) : Collections.emptyList();
        metadata = metadata != null ? metadata : DecisionMetadata.of(context.decisionType());
    }

    public static <T> DecisionRequest<T> of(DecisionContext context, List<T> candidates) {
        return new DecisionRequest<>(context, candidates, DecisionMetadata.of(context.decisionType()));
    }
}
