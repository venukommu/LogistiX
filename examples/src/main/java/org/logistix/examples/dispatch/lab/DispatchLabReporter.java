package org.logistix.examples.dispatch.lab;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * High-clarity reporter formatting DispatchComparisonResult into human-readable terminal boxes
 * (ready for 1080p screen recording), scenario summary tables, and structured JSON.
 */
public final class DispatchLabReporter {

    private static final ObjectMapper JSON_MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    private DispatchLabReporter() {}

    /**
     * Formats an executive summary table of all compared scenarios.
     */
    public static String formatScenarioSummary(List<DispatchComparisonResult> results) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        sb.append("=====================================================================================================================\n");
        sb.append("   LOGISTIX DRIVER DISPATCH DECISION LAB — KNOWLEDGE & AI COMPARISON SUMMARY\n");
        sb.append("=====================================================================================================================\n");
        sb.append(String.format("%-28s | %-16s | %-15s | %-11s | %-18s | %-8s\n",
                "Scenario ID", "Rec Changed?", "AI Influenced?", "AI Calls", "Knowledge Evidence", "Safety"));
        sb.append("---------------------------------------------------------------------------------------------------------------------\n");

        for (DispatchComparisonResult r : results) {
            String knowledgeStr = r.knowledgeEvidenceCount() > 0
                    ? String.format("%d docs (%s)", r.knowledgeEvidenceCount(), String.join(",", r.knowledgeEvidenceIds()))
                    : "0 docs";

            sb.append(String.format("%-28s | %-16s | %-15s | %-11d | %-18s | %-8s\n",
                    r.scenario().scenarioId(),
                    r.recommendationChanged() ? "YES (" + truncate(r.hybridDriver(), 10) + ")" : "NO (" + truncate(r.rulesOnlyDriver(), 10) + ")",
                    r.aiInfluencedDecision() ? "YES" : "NO",
                    r.aiInvocations(),
                    truncate(knowledgeStr, 18),
                    r.hardConstraintsSatisfied() ? "PASS ✓" : "FAIL ✗"));
        }

        sb.append("=====================================================================================================================\n");
        return sb.toString();
    }

    /**
     * Formats the comparison in a high-clarity side-by-side terminal box (1080p recording ready).
     */
    public static String formatSideBySideBox(DispatchComparisonResult result) {
        StringBuilder sb = new StringBuilder();

        sb.append("\n");
        sb.append("╔══════════════════════════════════════════════════════════════════════════════════════════════════════╗\n");
        sb.append(String.format("║  LOGISTIX DECISION LAB — %-76s║\n", result.scenario().name()));
        sb.append("╠══════════════════════════════════════════════════════════════════════════════════════════════════════╣\n");
        sb.append(String.format("║  Scenario ID : %-25s  Weather Advisory : %-43s║\n",
                result.scenario().scenarioId(), result.scenario().weatherAdvisory()));
        sb.append(String.format("║  Corridor    : %-86s║\n",
                truncate(result.scenario().corridorNotes(), 86)));
        sb.append("╠══════════════════════════════════════════════╦═══════════════════════════════════════════════════════╣\n");
        sb.append("║  WITHOUT KNOWLEDGE / RULES ONLY              ║  WITH KNOWLEDGE & HYBRID AI DECISION INTELLIGENCE     ║\n");
        sb.append("╠══════════════════════════════════════════════╬═══════════════════════════════════════════════════════╣\n");
        sb.append(String.format("║  Driver       : %-28s ║  Driver       : %-37s ║\n",
                truncate(result.rulesOnlyDriver(), 28), truncate(result.hybridDriver(), 37)));
        sb.append(String.format("║  Score        : %-28.3f ║  Score        : %-37.3f ║\n",
                result.rulesOnlyScore(), result.hybridScore()));
        sb.append(String.format("║  Confidence   : %-27.1f%% ║  Decision Conf: %-36.1f%% ║\n",
                result.rulesOnlyConfidence() * 100.0, result.hybridConfidence() * 100.0));
        sb.append(String.format("║  AI Calls     : %-28d ║  AI Calls     : %-37d ║\n",
                0, result.aiInvocations()));
        sb.append(String.format("║  Knowledge    : %-28s ║  Knowledge    : %-37s ║\n",
                "0 documents", result.knowledgeEvidenceCount() + " docs (" + result.knowledgeLatency().toMillis() + " ms)"));
        sb.append(String.format("║  AI Latency   : %-28s ║  AI Latency   : %-37s ║\n",
                "0 ms", result.aiLatency().toMillis() + " ms (" + result.aiProviderType() + ")"));
        sb.append(String.format("║  Evaluation   : %-28s ║  Advisory Conf: %-36.1f%% ║\n",
                "Deterministic Rules", result.aiAdvisoryConfidence() * 100.0));
        sb.append("╠══════════════════════════════════════════════╩═══════════════════════════════════════════════════════╣\n");
        sb.append("║  WHAT CHANGED?                                                                                       ║\n");
        sb.append("╠══════════════════════════════════════════════════════════════════════════════════════════════════════╣\n");

        sb.append(String.format("║  • Recommendation Changed : %-73s║\n", result.recommendationChanged() ? "YES" : "NO"));
        sb.append(String.format("║  • AI Influenced Decision : %-73s║\n", result.aiInfluencedDecision() ? "YES" : "NO"));
        sb.append(String.format("║  • Decision Policy Reason : %-73s║\n", truncate(result.aiInfluenceReason(), 73)));
        sb.append(String.format("║  • Regulatory Safety      : %-73s║\n",
                result.hardConstraintsSatisfied() ? "SAFE (All Hard Feasibility Constraints Satisfied ✓)" : "VIOLATION DETECTED ✗"));

        if (!result.knowledgeEvidenceIds().isEmpty()) {
            sb.append(String.format("║  • Knowledge Evidence     : %-73s║\n", String.join(", ", result.knowledgeEvidenceIds())));
        }

        if (!result.aiInsights().isEmpty()) {
            sb.append("║  • Grounded Insights & Explainability:                                                               ║\n");
            for (String insight : result.aiInsights()) {
                sb.append(String.format("║    - %-96s║\n", truncate(insight, 96)));
            }
        }

        sb.append("╚══════════════════════════════════════════════════════════════════════════════════════════════════════╝\n");
        return sb.toString();
    }

    /**
     * Formats the comparison as structured JSON.
     */
    public static String formatJson(DispatchComparisonResult result) {
        try {
            Map<String, Object> root = new LinkedHashMap<>();
            root.put("scenarioId", result.scenario().scenarioId());
            root.put("scenarioName", result.scenario().name());
            root.put("weatherAdvisory", result.scenario().weatherAdvisory());
            root.put("trafficRiskLevel", result.scenario().trafficRiskLevel());

            Map<String, Object> rulesOnly = new LinkedHashMap<>();
            rulesOnly.put("recommendation", result.rulesOnlyDriver());
            rulesOnly.put("score", result.rulesOnlyScore());
            rulesOnly.put("confidence", result.rulesOnlyConfidence());
            rulesOnly.put("aiInvocationCount", 0);
            rulesOnly.put("aiLatencyMs", 0);
            rulesOnly.put("knowledgeEvidenceCount", 0);
            root.put("rulesOnly", rulesOnly);

            Map<String, Object> knowledge = new LinkedHashMap<>();
            knowledge.put("provider", result.knowledgeProvider());
            knowledge.put("status", result.knowledgeStatus());
            knowledge.put("evidenceCount", result.knowledgeEvidenceCount());
            knowledge.put("evidenceIds", result.knowledgeEvidenceIds());
            knowledge.put("latencyMs", result.knowledgeLatency().toMillis());
            root.put("knowledge", knowledge);

            Map<String, Object> hybrid = new LinkedHashMap<>();
            hybrid.put("recommendation", result.hybridDriver());
            hybrid.put("score", result.hybridScore());
            hybrid.put("confidence", result.hybridConfidence());
            hybrid.put("aiAdvisoryConfidence", result.aiAdvisoryConfidence());
            hybrid.put("aiInvocationCount", result.aiInvocations());
            hybrid.put("aiLatencyMs", result.aiLatency().toMillis());
            hybrid.put("provider", result.aiProvider());
            hybrid.put("providerType", result.aiProviderType());
            hybrid.put("fallback", result.fallbackTriggered());
            root.put("hybrid", hybrid);

            Map<String, Object> comparison = new LinkedHashMap<>();
            comparison.put("recommendationChanged", result.recommendationChanged());
            comparison.put("previousRecommendation", result.previousRecommendation());
            comparison.put("finalRecommendation", result.finalRecommendation());
            comparison.put("aiInfluencedDecision", result.aiInfluencedDecision());
            comparison.put("aiInfluenceReason", result.aiInfluenceReason());
            comparison.put("safetyStatus", result.safetyStatus());
            comparison.put("scoreDifference", result.scoreDifference());
            comparison.put("aiInsights", result.aiInsights());
            root.put("comparison", comparison);

            return JSON_MAPPER.writeValueAsString(root);
        } catch (Exception e) {
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }

    private static String truncate(String text, int maxLen) {
        if (text == null) return "";
        if (text.length() <= maxLen) return text;
        return text.substring(0, maxLen - 3) + "...";
    }
}
