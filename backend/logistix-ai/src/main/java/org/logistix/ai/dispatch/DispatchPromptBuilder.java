package org.logistix.ai.dispatch;

import org.logistix.domain.decision.DecisionContext;

import java.util.Objects;

/**
 * Dedicated prompt builder constructing controlled system and user prompts for operational dispatch reasoning.
 */
public final class DispatchPromptBuilder {

    private static final String SYSTEM_PROMPT = """
            You are an operational logistics Decision Intelligence Advisor in the LogistiX framework.
            Your role is to analyze feasible driver candidate pairings for a given shipment order and provide qualitative risk assessments.
            
            NON-NEGOTIABLE OPERATIONAL RULES:
            1. You are strictly an ADVISOR. You do NOT make the final dispatch decision.
            2. You MUST NOT override or relax hard feasibility constraints (Hours of Service, vehicle payload capacity, required cargo certifications, SLA delivery deadlines).
            3. You MUST NOT invent or hallucinate drivers, certifications, or geographic facts.
            4. Distinguish verified deterministic facts from qualitative environmental assumptions (e.g. weather impacts, traffic delays).
            5. Provide a clear, actionable operational rationale, assess the risk level, and output your recommendation as valid JSON.
            
            JSON RESPONSE SCHEMA:
            {
              "candidateId": "string (UUID or driver ID)",
              "riskLevel": "LOW | MEDIUM | HIGH | CRITICAL",
              "advisoryConfidence": number (between 0.0 and 1.0),
              "reasoning": "string (concise narrative explaining operational risks and trade-offs)",
              "contributingFactors": ["string"],
              "warnings": ["string"],
              "suggestedScoreAdjustment": number (between -0.20 and +0.20)
            }
            """;

    private DispatchPromptBuilder() {}

    public static String getSystemPrompt() {
        return SYSTEM_PROMPT;
    }

    public static String buildUserPrompt(DecisionContext context, Object candidateObj) {
        Objects.requireNonNull(context, "DecisionContext must not be null");

        String weather = context.getEnvironmentAttribute("weatherAdvisory", String.class).orElse("CLEAR");
        String executionMode = context.getParameter("executionMode", String.class).orElse("HYBRID");

        StringBuilder sb = new StringBuilder();
        sb.append("Please evaluate the following feasible commercial driver candidate for dispatch assignment:\n\n");
        sb.append("--- ENVIRONMENTAL STATE ---\n");
        sb.append("Weather Advisory: ").append(weather).append("\n");
        sb.append("Execution Mode: ").append(executionMode).append("\n\n");

        sb.append("--- CANDIDATE DATA ---\n");
        if (candidateObj != null) {
            sb.append(candidateObj.toString()).append("\n");
        } else {
            sb.append("No candidate object provided.\n");
        }

        sb.append("\nEvaluate candidate route conditions, weather risk, and provide structured operational advice in the specified JSON format.");
        return sb.toString();
    }
}
