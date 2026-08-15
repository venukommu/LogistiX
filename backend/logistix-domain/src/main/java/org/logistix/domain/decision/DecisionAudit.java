package org.logistix.domain.decision;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Detailed step-by-step audit trace for regulatory compliance and operational transparency.
 */
public record DecisionAudit(
        int evaluatedRuleCount,
        int passedRuleCount,
        int violatedConstraintCount,
        long aiTokensConsumed,
        Map<String, Duration> stepDurations,
        List<String> auditLogs
) {
    public DecisionAudit {
        stepDurations = stepDurations != null ? Map.copyOf(stepDurations) : Collections.emptyMap();
        auditLogs = auditLogs != null ? List.copyOf(auditLogs) : Collections.emptyList();
    }

    public static DecisionAudit empty() {
        return new DecisionAudit(0, 0, 0, 0, Collections.emptyMap(), Collections.emptyList());
    }
}
