package org.logistix.examples.dispatch.knowledge;

import org.logistix.domain.decision.DecisionContext;
import org.logistix.domain.fact.Fact;
import org.logistix.domain.ports.KnowledgeProvider;
import org.logistix.domain.ports.KnowledgeProvider.GroundingDocument;
import org.logistix.domain.ports.KnowledgeProvider.KnowledgeQuery;
import org.logistix.engine.steps.DecisionStep;
import org.logistix.engine.steps.StepMetadata;
import org.logistix.engine.steps.StepResult;
import org.logistix.rag.knowledge.InMemoryKnowledgeProvider;
import org.logistix.rag.knowledge.KnowledgeTelemetry;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

/**
 * Pipeline step retrieving enterprise domain knowledge before AI contextual advisory.
 * Decoupled, failsafe step ensuring that missing knowledge never crashes an operational decision.
 */
public class DriverDispatchKnowledgeStep implements DecisionStep {

    public static final String STEP_ID = "step-dispatch-knowledge";
    public static final String STEP_NAME = "Enterprise Knowledge Retrieval";

    private final KnowledgeProvider knowledgeProvider;
    private final int maxResults;

    public DriverDispatchKnowledgeStep() {
        this(new InMemoryKnowledgeProvider(), 3);
    }

    public DriverDispatchKnowledgeStep(KnowledgeProvider knowledgeProvider) {
        this(knowledgeProvider, 3);
    }

    public DriverDispatchKnowledgeStep(KnowledgeProvider knowledgeProvider, int maxResults) {
        this.knowledgeProvider = knowledgeProvider != null ? knowledgeProvider : new InMemoryKnowledgeProvider();
        this.maxResults = Math.max(1, maxResults);
    }

    @Override
    public StepMetadata getMetadata() {
        return StepMetadata.optional(STEP_ID, STEP_NAME, 35);
    }

    @Override
    public StepResult execute(DecisionContext context) {
        Instant start = Instant.now();
        String correlationId = context.getParameter("correlationId", String.class).orElse(context.contextId().toString());

        try {
            String weather = context.getEnvironmentAttribute("weatherAdvisory", String.class).orElse("");
            String traffic = context.getEnvironmentAttribute("trafficRiskLevel", String.class).orElse("");
            String corridorNotes = context.getEnvironmentAttribute("corridorNotes", String.class).orElse("");

            String queryText = String.format("%s %s %s %s", weather, traffic, corridorNotes, context.decisionType()).trim();
            KnowledgeQuery query = KnowledgeQuery.of(queryText, "LOGISTICS", maxResults);

            List<GroundingDocument> evidence = knowledgeProvider.retrieveKnowledge(query);
            if (evidence.isEmpty()) {
                evidence = knowledgeProvider.retrieveKnowledge(context, maxResults);
            }

            Duration latency = Duration.between(start, Instant.now());
            List<String> docIds = evidence.stream().map(GroundingDocument::documentId).toList();

            KnowledgeTelemetry telemetry = KnowledgeTelemetry.success(
                    knowledgeProvider.getProviderName(),
                    queryText,
                    maxResults,
                    docIds,
                    latency,
                    correlationId
            );

            DecisionContext updatedContext = context
                    .withFact(Fact.of("knowledgeEvidence", evidence))
                    .withFact(Fact.of("knowledgeTelemetry", telemetry))
                    .withFact(Fact.of("knowledgeStatus", telemetry.status()));

            return StepResult.success(
                    updatedContext,
                    latency,
                    List.of(Fact.of("knowledgeEvidence", evidence), Fact.of("knowledgeTelemetry", telemetry)),
                    String.format("Retrieved %d grounding evidence documents via %s in %d ms",
                            evidence.size(), knowledgeProvider.getProviderName(), latency.toMillis())
            );

        } catch (Exception e) {
            Duration latency = Duration.between(start, Instant.now());
            KnowledgeTelemetry telemetry = KnowledgeTelemetry.fallback(
                    knowledgeProvider.getProviderName(),
                    "knowledge-query-error",
                    maxResults,
                    latency,
                    e.getMessage(),
                    correlationId
            );

            DecisionContext updatedContext = context
                    .withFact(Fact.of("knowledgeEvidence", Collections.<GroundingDocument>emptyList()))
                    .withFact(Fact.of("knowledgeTelemetry", telemetry))
                    .withFact(Fact.of("knowledgeStatus", "FALLBACK_TRIGGERED"));

            return StepResult.success(
                    updatedContext,
                    latency,
                    String.format("Knowledge retrieval degraded gracefully to fallback: %s", e.getMessage())
            );
        }
    }
}
