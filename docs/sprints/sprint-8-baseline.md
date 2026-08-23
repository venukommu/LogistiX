# Sprint 8 Baseline Report: Driver Dispatch Decision Lab

## 1. Executive Summary
Following the completion and architectural closure of Sprint 7.x (milestone `2272b88`), the **AI-Assisted Commercial Driver Dispatch** capability is established as the LogistiX Golden Reference Capability.

**Sprint 8** introduces the **Driver Dispatch Decision Lab** — an interactive comparison engine, CLI demonstration, and benchmarking harness that measures and visualizes the exact differences between:
- **`RULES_ONLY`**: Zero-AI, pure deterministic constraint evaluation, business rules, and multi-criteria scoring.
- **`HYBRID_AI`**: Single-call batched AI contextual reasoning layered on top of deterministic guardrails and scoring.

Both execution modes run on **identical scenario inputs** to prove the foundational LogistiX thesis:
> *"The AI can reason. LogistiX decides."*

---

## 2. Review of Existing Components

| Component | Responsibility in Sprint 7.x | Reusability in Sprint 8 Decision Lab |
| :--- | :--- | :--- |
| `DispatchDecisionPipelineFactory` | Creates `RULES_ONLY` and `HYBRID` pipelines. | Authoritative factory for executing comparison pipelines. |
| `DriverDispatchAIStep` | Single-call batched AI evaluation with `AITelemetry`. | Core advisory stage in `HYBRID_AI` mode. |
| `DriverDispatchRecommendationStep` | Produces `DispatchAssignment`, `Explanation`, and attributes metadata. | Generates the decision outcome for both modes. |
| `MockDispatchAIProvider` | Deterministic offline AI provider tracking invocation count. | Powers offline scenario comparisons in CI and Lab demonstrations. |
| `SpringAIDispatchAIProvider` | Real Spring AI ChatModel integration. | Powers optional live model comparisons. |
| `DriverDispatchGoldenReferenceTest` | Regression suite verifying constraints and invariants. | Guarantees zero regression while building the Decision Lab. |

---

## 3. Current Limitations & Sprint 8 Target Architecture

### 3.1 Limitations in Current Demonstration
1. No standardized abstraction for reusable operational scenarios (currently hardcoded inside `DriverDispatchReferenceApp`).
2. No automated side-by-side diff engine comparing `RULES_ONLY` vs `HYBRID_AI` metrics on identical inputs.
3. No dedicated CLI flags to select scenarios or format outputs as JSON/side-by-side terminal tables.

### 3.2 Target Comparison Flow
```
                      DispatchScenario (Immutable Input)
                                    │
                  ┌─────────────────┴─────────────────┐
                  ▼                                   ▼
         RULES_ONLY Pipeline                 HYBRID_AI Pipeline
      (Constraints → Rules → Scoring)     (Constraints → Rules → Scoring → AI)
                  │                                   │
                  ▼                                   ▼
           DecisionResult                      DecisionResult
          (AI Invocations: 0)                 (AI Invocations: 1)
                  │                                   │
                  └─────────────────┬─────────────────┘
                                    ▼
                        DispatchComparisonResult
                 (Side-by-Side Metrics, Diff, Telemetry)
                                    │
                                    ▼
                Terminal Box View / JSON / Benchmark Report
```

---

## 4. Verification Check
Running baseline test suite to confirm starting status.
