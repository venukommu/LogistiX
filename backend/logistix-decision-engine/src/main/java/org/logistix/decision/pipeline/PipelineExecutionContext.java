package org.logistix.decision.pipeline;

import org.logistix.domain.decision.DecisionContext;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Mutable/evolving execution state passed sequentially across pipeline stages:
 * ConstraintStage &rarr; RuleStage &rarr; AIStage &rarr; ScoringStage &rarr; RecommendationStage.
 *
 * @param <C> Candidate type
 */
public record PipelineExecutionContext<C>(
        DecisionContext context,
        List<C> activeCandidates,
        Map<String, Object> stageOutputs,
        Map<String, Duration> stageMetrics
) {
    public PipelineExecutionContext {
        Objects.requireNonNull(context, "Context must not be null");
        activeCandidates = activeCandidates != null ? List.copyOf(activeCandidates) : Collections.emptyList();
        stageOutputs = stageOutputs != null ? Collections.unmodifiableMap(new LinkedHashMap<>(stageOutputs)) : Collections.emptyMap();
        stageMetrics = stageMetrics != null ? Collections.unmodifiableMap(new LinkedHashMap<>(stageMetrics)) : Collections.emptyMap();
    }

    public static <C> PipelineExecutionContext<C> of(DecisionContext context, List<C> initialCandidates) {
        return new PipelineExecutionContext<>(context, initialCandidates, Collections.emptyMap(), Collections.emptyMap());
    }

    public PipelineExecutionContext<C> withActiveCandidates(List<C> filteredCandidates) {
        return new PipelineExecutionContext<>(this.context, filteredCandidates, this.stageOutputs, this.stageMetrics);
    }

    public PipelineExecutionContext<C> withStageOutput(String stageName, Object output, Duration duration) {
        Map<String, Object> newOutputs = new LinkedHashMap<>(this.stageOutputs);
        newOutputs.put(stageName, output);

        Map<String, Duration> newMetrics = new LinkedHashMap<>(this.stageMetrics);
        newMetrics.put(stageName, duration);

        return new PipelineExecutionContext<>(this.context, this.activeCandidates, newOutputs, newMetrics);
    }
}
