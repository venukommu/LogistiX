package org.logistix.domain.decision;

import org.logistix.domain.constraint.ConstraintViolation;
import org.logistix.domain.explanation.Explanation;
import org.logistix.domain.recommendation.Recommendation;
import org.logistix.domain.rule.RuleOutcome;
import org.logistix.domain.score.Score;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Immutable outcome of executing a complete decision pipeline.
 *
 * @param <T> The target domain recommendation type
 */
public record DecisionResult<T>(
        String decisionType,
        Recommendation<T> recommendation,
        double confidence,
        Score score,
        Explanation explanation,
        List<RuleOutcome> executedRules,
        List<ConstraintViolation> violatedConstraints,
        DecisionMetadata metadata,
        DecisionAudit audit,
        Duration executionTime
) {
    public DecisionResult {
        Objects.requireNonNull(decisionType, "Decision type must not be null");
        Objects.requireNonNull(recommendation, "Recommendation must not be null");
        Objects.requireNonNull(score, "Score must not be null");
        Objects.requireNonNull(explanation, "Explanation must not be null");
        Objects.requireNonNull(metadata, "Metadata must not be null");
        Objects.requireNonNull(executionTime, "Execution time must not be null");
        executedRules = executedRules != null ? List.copyOf(executedRules) : Collections.emptyList();
        violatedConstraints = violatedConstraints != null ? List.copyOf(violatedConstraints) : Collections.emptyList();
        audit = audit != null ? audit : DecisionAudit.empty();
    }
}
