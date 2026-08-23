# Sprint 8.1 Baseline Report: Decision Lab Evidence & Demo Hardening

## 1. Executive Summary
Following the completion of Sprint 8 (commit `47385d7`), the **Driver Dispatch Decision Lab** (`org.logistix.examples.dispatch.lab`) successfully demonstrated side-by-side execution between `RULES_ONLY` (0 AI calls) and `HYBRID_AI` (1 batched AI call).

**Sprint 8.1** hardens the Decision Lab in four primary areas:
1. **Benchmark Integrity**: Correct benchmark labeling to prevent in-memory Mock AI from being misinterpreted as real LLM inference speed. Introduce explicit `AI Overhead` accounting for `HYBRID_LIVE`.
2. **AI Differentiation Scenario (Scenario 4: `ai-contextual-decision`)**: Introduce a deterministic scenario where both Driver A and Driver B are 100% hard-feasible, `RULES_ONLY` selects Driver A based on deadhead proximity, and `HYBRID_AI` selects Driver B because AI identifies severe corridor weather/SLA risk that the deterministic selection policy interprets safely.
3. **Explicit Comparison Metrics & Safety Reporting**: Add `aiInfluencedDecision`, `aiInfluenceReason`, and `safetyStatus` (`SAFE`, `REJECTED_AI_RECOMMENDATION`, `FALLBACK`) to `DispatchComparisonResult`.
4. **Demo-Ready Terminal Output & Scenario Summary**: Provide an immediate comparative summary table when running `--compare --scenario all` and format 1080p screen-recording-ready box views.

---

## 2. Review of Current Sprint 8 Components

| Component | Current Sprint 8 Status | Sprint 8.1 Hardening Target |
| :--- | :--- | :--- |
| `DispatchScenarios` | 3 Scenarios (`baseline-clear`, `corridor-weather-risk`, `safety-constraint-protection`). None changed recommendation from Driver A to Driver B. | Add Scenario 4 (`ai-contextual-decision`) where AI contextual risk shifts the deterministic policy selection between two hard-feasible candidates. |
| `DriverDispatchRecommendationStep` | Picks top scored candidate, appends AI context to explanation. | Incorporate deterministic policy evaluation of AI risk signals for Top-N feasible candidates. |
| `DispatchComparisonResult` | Contains `recommendationChanged`, scores, latency, telemetry. | Add `aiInfluencedDecision`, `aiInfluenceReason`, `safetyStatus`, `previousRecommendation`, `finalRecommendation`. |
| `DispatchLabReporter` | Formats single box views and JSON. | Add Scenario Summary table mode and enriched comparison metrics in text and JSON. |
| `DispatchBenchmarkRunner` | Reports throughput and latency for JVM vs Mock AI without distinction. | Explicitly label Mock AI as "Orchestration test only", add optional live LLM latency, and compute `AI Overhead`. |

---

## 3. Inviolable Architectural Principles
1. *"The AI can reason. LogistiX decides."*
2. AI does not directly modify raw mathematical scores. AI emits qualitative risk signals (`riskLevel`, `advisoryConfidence`, `reasoning`).
3. The deterministic selection policy evaluates verified qualitative signals among already HARD-feasible candidates.
4. AI cannot override feasibility constraints or resurrect unfeasible drivers.
5. `RULES_ONLY` executes with exactly 0 AI invocations. `HYBRID_AI` executes with exactly 1 batched AI invocation.

---

## 4. Verification Check
Running baseline `mvn clean test` to confirm clean starting state.
