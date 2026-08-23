package org.logistix.examples.dispatch.scoring;

import org.logistix.domain.decision.DecisionContext;
import org.logistix.domain.fact.Fact;
import org.logistix.domain.score.Score;
import org.logistix.domain.score.ScoringEngine;
import org.logistix.engine.steps.ScoringStep;
import org.logistix.engine.steps.StepMetadata;
import org.logistix.engine.steps.StepResult;
import org.logistix.examples.dispatch.model.DispatchCandidate;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Pipeline step applying multi-criteria scoring to all feasible dispatch candidates and ranking them.
 */
public class DriverDispatchScoringStep implements ScoringStep {

    public static final String STEP_ID = "step-dispatch-scoring";
    public static final String STEP_NAME = "Driver Dispatch Multi-Criteria Scoring";

    private final ScoringEngine<DispatchCandidate> scoringEngine;

    public DriverDispatchScoringStep() {
        this(new DispatchScoringEngine());
    }

    public DriverDispatchScoringStep(ScoringEngine<DispatchCandidate> scoringEngine) {
        this.scoringEngine = scoringEngine;
    }

    @Override
    public StepMetadata getMetadata() {
        return StepMetadata.of(STEP_ID, STEP_NAME, 30);
    }

    @Override
    @SuppressWarnings("unchecked")
    public StepResult execute(DecisionContext context) {
        Instant start = Instant.now();

        List<DispatchCandidate> candidates = context.getFactValue("feasibleCandidates", List.class)
                .orElse(Collections.emptyList());

        List<DispatchCandidate> scoredCandidates = new ArrayList<>();

        for (DispatchCandidate candidate : candidates) {
            Score score = scoringEngine.score(candidate, context);
            scoredCandidates.add(candidate.withScore(score));
        }

        // Sort descending by score value
        scoredCandidates.sort(Comparator.comparingDouble((DispatchCandidate c) -> c.score().value()).reversed());

        Duration duration = Duration.between(start, Instant.now());

        DecisionContext updatedContext = context
                .withFact(Fact.of("rankedCandidates", scoredCandidates));

        String bestScoreMsg = scoredCandidates.isEmpty() ? "none" :
                String.format("%.3f (%s)", scoredCandidates.get(0).score().value(), scoredCandidates.get(0).driver().name());

        return StepResult.success(
                updatedContext,
                duration,
                List.of(Fact.of("rankedCandidates", scoredCandidates)),
                String.format("Scored and ranked %d candidates. Top candidate: %s", scoredCandidates.size(), bestScoreMsg)
        );
    }
}
