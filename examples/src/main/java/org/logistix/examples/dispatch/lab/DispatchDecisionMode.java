package org.logistix.examples.dispatch.lab;

/**
 * Execution modes evaluated in the Driver Dispatch Decision Lab.
 */
public enum DispatchDecisionMode {
    /**
     * Purely deterministic evaluation (Constraints -> Rules -> Multi-Criteria Scoring -> Recommendation).
     * Guaranteed ZERO AI invocations.
     */
    RULES_ONLY,

    /**
     * Hybrid evaluation (Constraints -> Rules -> Multi-Criteria Scoring -> Single Batched AI -> Recommendation).
     * Guaranteed EXACTLY ONE batched AI invocation by default.
     */
    HYBRID_AI
}
