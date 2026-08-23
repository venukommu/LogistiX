package org.logistix.examples.dispatch.rules;

import org.logistix.domain.decision.DecisionContext;
import org.logistix.domain.fact.Fact;
import org.logistix.domain.rule.Rule;
import org.logistix.domain.rule.RuleOutcome;
import org.logistix.engine.steps.RuleStep;
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
 * Pipeline step executing prioritized operational and business rules for Driver Dispatch candidates.
 */
public class DriverDispatchRuleStep implements RuleStep {

    public static final String STEP_ID = "step-dispatch-rules";
    public static final String STEP_NAME = "Driver Dispatch Business Rules Evaluation";

    private final List<Rule<DispatchCandidate>> rules;

    public DriverDispatchRuleStep() {
        this(List.of(
                new PreferredDriverRule(),
                new RestBalanceRule(),
                new RegionalAffinityRule()
        ));
    }

    public DriverDispatchRuleStep(List<Rule<DispatchCandidate>> rules) {
        List<Rule<DispatchCandidate>> sorted = new ArrayList<>(rules);
        sorted.sort(Comparator.comparingInt(Rule<DispatchCandidate>::getPriority).reversed());
        this.rules = List.copyOf(sorted);
    }

    @Override
    public StepMetadata getMetadata() {
        return StepMetadata.of(STEP_ID, STEP_NAME, 20);
    }

    @Override
    @SuppressWarnings("unchecked")
    public StepResult execute(DecisionContext context) {
        Instant start = Instant.now();

        List<DispatchCandidate> feasibleCandidates = context.getFactValue("feasibleCandidates", List.class)
                .orElse(Collections.emptyList());

        List<DispatchCandidate> evaluatedCandidates = new ArrayList<>();
        List<RuleOutcome> allOutcomes = new ArrayList<>();

        for (DispatchCandidate candidate : feasibleCandidates) {
            List<RuleOutcome> candidateOutcomes = new ArrayList<>();
            for (Rule<DispatchCandidate> rule : rules) {
                RuleOutcome outcome = rule.evaluate(candidate, context);
                candidateOutcomes.add(outcome);
                allOutcomes.add(outcome);
            }
            evaluatedCandidates.add(candidate.withRuleOutcomes(candidateOutcomes));
        }

        Duration duration = Duration.between(start, Instant.now());

        DecisionContext updatedContext = context
                .withFact(Fact.of("feasibleCandidates", evaluatedCandidates))
                .withFact(Fact.of("executedRules", allOutcomes));

        return StepResult.success(
                updatedContext,
                duration,
                List.of(Fact.of("feasibleCandidates", evaluatedCandidates)),
                String.format("Business rules evaluated across %d candidates (%d rule evaluations total)",
                        evaluatedCandidates.size(), allOutcomes.size())
        );
    }
}
