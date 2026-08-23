package org.logistix.examples.dispatch.lab;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * High-clarity reporter formatting DispatchComparisonResult into human-readable terminal boxes
 * (ready for 1080p screen recording) or structured JSON.
 */
public final class DispatchLabReporter {

    private static final ObjectMapper JSON_MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    private DispatchLabReporter() {}

    /**
     * Formats the comparison in a high-clarity side-by-side terminal box.
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
        sb.append("║  WITHOUT AI (RULES ONLY)                     ║  WITH AI (HYBRID DECISION INTELLIGENCE)               ║\n");
        sb.append("╠══════════════════════════════════════════════╬═══════════════════════════════════════════════════════╣\n");
        sb.append(String.format("║  Driver       : %-28s ║  Driver       : %-37s ║\n",
                truncate(result.rulesOnlyDriver(), 28), truncate(result.hybridDriver(), 37)));
        sb.append(String.format("║  Score        : %-28.3f ║  Score        : %-37.3f ║\n",
                result.rulesOnlyScore(), result.hybridScore()));
        sb.append(String.format("║  Confidence   : %-27.1f%% ║  Decision Conf: %-36.1f%% ║\n",
                result.rulesOnlyConfidence() * 100.0, result.hybridConfidence() * 100.0));
        sb.append(String.format("║  AI Calls     : %-28d ║  AI Calls     : %-37d ║\n",
                0, result.aiInvocations()));
        sb.append(String.format("║  AI Latency   : %-28s ║  AI Latency   : %-37s ║\n",
                "0 ms", result.aiLatency().toMillis() + " ms (" + result.aiProviderType() + ")"));
        sb.append(String.format("║  Evaluation   : %-28s ║  Advisory Conf: %-36.1f%% ║\n",
                "Deterministic Rules", result.aiAdvisoryConfidence() * 100.0));
        sb.append("╠══════════════════════════════════════════════╩═══════════════════════════════════════════════════════╣\n");
        sb.append("║  WHAT CHANGED?                                                                                       ║\n");
        sb.append("╠══════════════════════════════════════════════════════════════════════════════════════════════════════╣\n");

        if (result.recommendationChanged()) {
            sb.append(String.format("║  • Recommendation Shift : %s -> %-62s║\n",
                    result.rulesOnlyDriver(), result.hybridDriver()));
            sb.append("║  • Reason               : AI identified qualitative corridor risk impacting deterministic policy.   ║\n");
        } else {
            sb.append(String.format("║  • Recommendation Shift : NONE (Both selected %-53s)║\n", result.rulesOnlyDriver()));
            sb.append("║  • Value Added          : AI confirmed deterministic recommendation and provided risk telemetry.     ║\n");
        }

        sb.append(String.format("║  • Regulatory Safety    : %-83s║\n",
                result.hardConstraintsSatisfied() ? "PASSED (All Hard Constraints Satisfied)" : "VIOLATION DETECTED"));

        if (!result.aiInsights().isEmpty()) {
            sb.append("║  • AI Contextual Insights:                                                                           ║\n");
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
            rulesOnly.put("driver", result.rulesOnlyDriver());
            rulesOnly.put("score", result.rulesOnlyScore());
            rulesOnly.put("decisionConfidence", result.rulesOnlyConfidence());
            rulesOnly.put("aiInvocations", 0);
            rulesOnly.put("aiLatencyMs", 0);
            root.put("rulesOnly", rulesOnly);

            Map<String, Object> hybrid = new LinkedHashMap<>();
            hybrid.put("driver", result.hybridDriver());
            hybrid.put("score", result.hybridScore());
            hybrid.put("decisionConfidence", result.hybridConfidence());
            hybrid.put("aiAdvisoryConfidence", result.aiAdvisoryConfidence());
            hybrid.put("aiInvocations", result.aiInvocations());
            hybrid.put("aiLatencyMs", result.aiLatency().toMillis());
            hybrid.put("aiProvider", result.aiProvider());
            hybrid.put("aiProviderType", result.aiProviderType());
            hybrid.put("aiInsights", result.aiInsights());
            hybrid.put("fallbackTriggered", result.fallbackTriggered());
            root.put("hybrid", hybrid);

            Map<String, Object> comparison = new LinkedHashMap<>();
            comparison.put("recommendationChanged", result.recommendationChanged());
            comparison.put("scoreDifference", result.scoreDifference());
            comparison.put("hardConstraintsSatisfied", result.hardConstraintsSatisfied());
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
