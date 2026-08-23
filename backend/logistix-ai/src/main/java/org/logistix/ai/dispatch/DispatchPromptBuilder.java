package org.logistix.ai.dispatch;

import org.logistix.domain.ports.KnowledgeProvider.GroundingDocument;

import java.util.Objects;

/**
 * Dedicated prompt builder constructing versioned, controlled system and user prompts
 * for single-call batched operational dispatch reasoning with enterprise knowledge grounding.
 */
public final class DispatchPromptBuilder {

    public static final String PROMPT_VERSION = "DRIVER_DISPATCH_AI_PROMPT_V2";

    private static final String SYSTEM_PROMPT = """
            You are an operational logistics Decision Intelligence Advisor in the LogistiX framework.
            Your role is to analyze a batch of FEASIBLE candidate driver pairings for a commercial shipment order and provide qualitative risk assessments.
            
            NON-NEGOTIABLE OPERATIONAL BOUNDARIES:
            1. You are strictly an ADVISOR. You do NOT define scoring weights or make the final binding dispatch assignment.
            2. Hard feasibility constraints (HOS limits, vehicle payload weight/volume, required endorsements, delivery deadlines) have ALREADY been strictly evaluated and enforced by the engine. You MUST NOT attempt to override them.
            3. You MUST NOT invent or hallucinate drivers, certifications, policies, or geographic route data.
            4. Distinguish verified deterministic facts from qualitative environmental assumptions (e.g. weather advisories, traffic bottlenecks).
            5. When retrieved enterprise knowledge evidence is provided, cite the relevant evidence IDs in "knowledgeEvidenceUsed". Do NOT cite or invent unknown evidence IDs.
            6. Provide structured advisory signals for each candidate in the batch, including risk level, advisory confidence, concise qualitative reasoning, warnings, and cited knowledge evidence.
            
            JSON RESPONSE SCHEMA:
            {
              "overallContextAssessment": "string (summary of environmental and corridor conditions)",
              "candidateAdvices": [
                {
                  "candidateId": "string (matching candidateId)",
                  "riskLevel": "LOW | MEDIUM | HIGH | CRITICAL",
                  "advisoryConfidence": number (between 0.0 and 1.0),
                  "reasoning": "string (concise qualitative rationale grounded in operational facts and knowledge)",
                  "contributingFactors": ["string"],
                  "warnings": ["string"],
                  "knowledgeEvidenceUsed": ["string (e.g. DOC-WINTER-001)"]
                }
              ]
            }
            """;

    private DispatchPromptBuilder() {}

    public static String getSystemPrompt() {
        return SYSTEM_PROMPT;
    }

    public static String getPromptVersion() {
        return PROMPT_VERSION;
    }

    public static String buildUserPrompt(DispatchAIRequest request) {
        Objects.requireNonNull(request, "DispatchAIRequest must not be null");

        StringBuilder sb = new StringBuilder();
        sb.append("Please evaluate the following shipment and candidate driver pairings:\n\n");
        sb.append("--- SHIPMENT ORDER ---\n");
        sb.append("Shipment ID: ").append(request.shipmentId()).append("\n");
        sb.append("Origin: ").append(request.originSummary()).append("\n");
        sb.append("Destination: ").append(request.destinationSummary()).append("\n");
        sb.append("Weight: ").append(request.weightKg()).append(" kg\n");
        sb.append("Delivery Deadline: ").append(request.deliveryDeadline()).append("\n");
        sb.append("Weather Advisory: ").append(request.weatherAdvisory()).append("\n");
        sb.append("Execution Mode: ").append(request.executionMode()).append("\n\n");

        if (request.knowledgeEvidence() != null && !request.knowledgeEvidence().isEmpty()) {
            sb.append("--- RETRIEVED ENTERPRISE KNOWLEDGE EVIDENCE (").append(request.knowledgeEvidence().size()).append(") ---\n");
            for (GroundingDocument doc : request.knowledgeEvidence()) {
                sb.append(String.format("Evidence [%s] - %s (Source: %s, Section: %s, Relevance: %.2f):\n",
                        doc.documentId(), doc.title(), doc.source(), doc.section(), doc.relevanceScore()));
                sb.append("  \"").append(doc.content()).append("\"\n\n");
            }
        }

        sb.append("--- FEASIBLE CANDIDATE DRIVERS (").append(request.candidates().size()).append(") ---\n");
        for (int i = 0; i < request.candidates().size(); i++) {
            CandidatePromptContext c = request.candidates().get(i);
            sb.append(String.format("Candidate #%d [%s]:\n", (i + 1), c.candidateId()));
            sb.append(String.format("  • Driver Name: %s (Tier: %s, Rating: %.2f/5.0, On-Time: %.0f%%)\n",
                    c.driverName(), c.driverTier(), c.driverRating(), c.historicalOnTimeRate() * 100.0));
            sb.append(String.format("  • Deadhead: %.1f km (%d min) | Linehaul: %d min | Scheduled Delivery: %s\n",
                    c.deadheadDistanceKm(), c.deadheadDurationMinutes(), c.linehaulDurationMinutes(), c.scheduledDeliveryTime()));
            sb.append(String.format("  • Deterministic Pre-Score: %.3f\n", c.deterministicScore()));
            if (!c.activeRuleSignals().isEmpty()) {
                sb.append("  • Active Business Rules: ").append(String.join("; ", c.activeRuleSignals())).append("\n");
            }
            sb.append("\n");
        }

        sb.append("Provide your qualitative risk assessment and contextual advice for each candidate strictly in the specified JSON format.");
        return sb.toString();
    }
}
