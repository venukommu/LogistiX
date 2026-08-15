package org.logistix.engine.executor;

import org.logistix.domain.decision.DecisionAudit;
import org.logistix.domain.decision.DecisionContext;
import org.logistix.domain.decision.DecisionMetadata;
import org.logistix.domain.decision.DecisionRequest;
import org.logistix.domain.decision.DecisionResponse;
import org.logistix.domain.decision.DecisionResult;
import org.logistix.domain.events.DecisionCompletedEvent;
import org.logistix.domain.events.DomainEventPublisher;
import org.logistix.domain.exceptions.EngineNotFoundException;
import org.logistix.domain.explanation.Explanation;
import org.logistix.domain.recommendation.Recommendation;
import org.logistix.domain.score.Score;
import org.logistix.engine.configuration.EngineConfiguration;
import org.logistix.engine.hooks.DecisionHook;
import org.logistix.engine.hooks.HookRegistry;
import org.logistix.engine.metrics.DecisionMetrics;
import org.logistix.engine.metrics.MetricsCollector;
import org.logistix.engine.metrics.StepMetrics;
import org.logistix.engine.pipeline.DecisionPipeline;
import org.logistix.engine.registry.DecisionRegistry;
import org.logistix.engine.steps.DecisionStep;
import org.logistix.engine.steps.StepResult;
import org.logistix.engine.steps.StepStatus;
import org.logistix.engine.trace.DecisionTraceEntry;
import org.logistix.engine.trace.TraceRecorder;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Standard framework pipeline executor orchestrating step execution, lifecycle hooks,
 * metrics collection, audit tracing, and domain event publication.
 */
public class DefaultDecisionExecutor implements DecisionExecutor {

    private final DecisionRegistry decisionRegistry;
    private final HookRegistry hookRegistry;
    private final EngineConfiguration configuration;
    private final DomainEventPublisher eventPublisher;

    public DefaultDecisionExecutor(
            DecisionRegistry decisionRegistry,
            HookRegistry hookRegistry,
            EngineConfiguration configuration,
            DomainEventPublisher eventPublisher
    ) {
        this.decisionRegistry = Objects.requireNonNull(decisionRegistry, "DecisionRegistry must not be null");
        this.hookRegistry = Objects.requireNonNull(hookRegistry, "HookRegistry must not be null");
        this.configuration = configuration != null ? configuration : EngineConfiguration.defaults();
        this.eventPublisher = eventPublisher;
    }

    @Override
    public <T> DecisionResult<T> execute(String decisionType, DecisionContext context) {
        DecisionPipeline pipeline = decisionRegistry.getPipeline(decisionType)
                .orElseThrow(() -> new EngineNotFoundException(decisionType));
        return execute(pipeline, context);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> DecisionResult<T> execute(DecisionPipeline pipeline, DecisionContext context) {
        Objects.requireNonNull(pipeline, "DecisionPipeline must not be null");
        Objects.requireNonNull(context, "DecisionContext must not be null");

        Instant startTime = Instant.now();
        List<DecisionHook> hooks = hookRegistry.getHooks();

        // 1. BeforeDecision Hook
        for (DecisionHook hook : hooks) {
            hook.beforeDecision(context);
        }

        DecisionContext currentContext = context;
        List<StepMetrics> stepMetricsList = new ArrayList<>();
        List<DecisionTraceEntry> traceEntries = new ArrayList<>();

        try {
            for (DecisionStep step : pipeline.steps()) {
                // BeforeStep Hook
                for (DecisionHook hook : hooks) {
                    hook.beforeStep(currentContext, step);
                }

                Instant stepStart = Instant.now();
                StepResult stepResult = step.execute(currentContext);
                Duration stepDuration = Duration.between(stepStart, Instant.now());

                // Record Step Metrics & Trace
                StepMetrics metrics = StepMetrics.of(
                        step.getMetadata().stepId(),
                        step.getMetadata().name(),
                        stepDuration,
                        stepResult.status() == StepStatus.SUCCESS
                );
                stepMetricsList.add(metrics);

                DecisionTraceEntry traceEntry = DecisionTraceEntry.of(
                        step.getMetadata().stepId(),
                        step.getMetadata().name(),
                        stepResult.status(),
                        stepDuration,
                        stepResult.message()
                );
                traceEntries.add(traceEntry);

                // AfterStep Hook
                for (DecisionHook hook : hooks) {
                    hook.afterStep(currentContext, step, stepResult);
                }

                if (stepResult.status() == StepStatus.FAILED && !step.getMetadata().optional()) {
                    throw new DecisionExecutionException(step.getMetadata().stepId(), stepResult.message());
                }

                currentContext = stepResult.context();

                if (stepResult.status() == StepStatus.SHORT_CIRCUIT) {
                    break;
                }
            }

            Duration totalDuration = Duration.between(startTime, Instant.now());

            // Extract or build recommendation from context
            Recommendation<T> recommendation = currentContext.getFactValue("recommendation", (Class<Recommendation<T>>) (Class<?>) Recommendation.class)
                    .orElseGet(() -> (Recommendation<T>) Recommendation.of(
                            "DEFAULT_RECOMMENDATION",
                            1,
                            Score.of(1.0, 1.0),
                            "Automated pipeline completion"
                    ));

            Explanation explanation = currentContext.getFactValue("explanation", Explanation.class)
                    .orElseGet(() -> Explanation.simple("Decision executed across " + pipeline.steps().size() + " steps.", 1.0));

            Score score = recommendation.score();
            double confidence = recommendation.confidence();

            DecisionMetadata metadata = DecisionMetadata.of(pipeline.decisionType(), context.contextId().toString());
            DecisionAudit audit = new DecisionAudit(0, 0, 0, 0, Collections.emptyMap(), Collections.emptyList());

            DecisionResult<T> result = new DecisionResult<>(
                    pipeline.decisionType(),
                    recommendation,
                    confidence,
                    score,
                    explanation,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    metadata,
                    audit,
                    totalDuration
            );

            // AfterDecision Hook
            for (DecisionHook hook : hooks) {
                hook.afterDecision(currentContext, result);
                hook.onDecisionCompleted(result);
            }

            // Publish Domain Event
            if (eventPublisher != null) {
                eventPublisher.publish(new DecisionCompletedEvent<>(result));
            }

            return result;

        } catch (Throwable t) {
            for (DecisionHook hook : hooks) {
                hook.onDecisionFailed(currentContext, t);
            }
            if (t instanceof RuntimeException re) {
                throw re;
            }
            throw new DecisionExecutionException("Pipeline execution failed: " + t.getMessage(), t);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <C, R> DecisionResponse<R> executeRequest(DecisionRequest<C> request) {
        DecisionResult<R> primary = execute(request.context().decisionType(), request.context());
        return DecisionResponse.of(primary);
    }
}
