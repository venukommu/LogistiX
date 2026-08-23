# Sprint 7.3 Baseline Report: Architecture Closure & Golden Reference Capability

## 1. Executive Summary
This document establishes the architecture baseline for **Sprint 7.3: Architecture Closure & Golden Reference Capability**.
Following Sprint 7.2 (commit `4dca590`), which hardened the AI decision boundary with single-call batched evaluation and typed telemetry, Sprint 7.3 performs the final architecture closure to establish Driver Dispatch as the **Golden Reference Capability** for LogistiX.

---

## 2. Current Implementation Audit & Identified Action Items

### 2.1 Complete Removal of Obsolete AI Score Authority
- **Current State**: `DispatchAIAdvice` still contains a `@Deprecated double suggestedScoreAdjustment` field (initialized to 0.0 in Sprint 7.2).
- **Target in Sprint 7.3**: Completely remove `suggestedScoreAdjustment` from `DispatchAIAdvice`, constructors, factory methods, and test fixtures. LogistiX deterministic policy is the sole authority over scoring.

### 2.2 Complete Removal of Legacy String-Dumping Prompt Paths
- **Current State**: `DispatchPromptBuilder` retains an overload `buildUserPrompt(DecisionContext, Object)` that falls back to `candidateObj.toString()`.
- **Target in Sprint 7.3**: Eliminate generic `Object` prompt overloads. `DispatchPromptBuilder` will exclusively accept strongly typed `DispatchAIRequest` objects.

### 2.3 Single AI Invocation Invariant
- **Current State**: `DriverDispatchAIStep` executes 1 batched call for top-N candidates.
- **Target in Sprint 7.3**: Codify and test edge cases: N = 0 (AI skipped with 0 invocations), N = 1, N = 3, N > 3 (bounded to top-N = 3).

### 2.4 Removal of Deprecated `DispatchAIAdvisor`
- **Current State**: `DispatchAIAdvisor` is marked `@Deprecated` and acts as a wrapper around `MockDispatchAIProvider`.
- **Target in Sprint 7.3**: Delete `DispatchAIAdvisor` and update any referencing tests to use `MockDispatchAIProvider` directly.

### 2.5 Batched AI Response Validation & Anomaly Guard
- **Current State**: `DriverDispatchAIStep` matches advice by `candidateId`.
- **Target in Sprint 7.3**: Explicitly validate response elements (reject unrecognized/phantom candidate IDs, ignore duplicates, clamp/validate confidence bounds `[0.0, 1.0]`) to ensure malformed or rogue AI responses never disrupt deterministic dispatch.

### 2.6 Telemetry Provider Type Transparency
- **Current State**: `AITelemetry` captures `providerName`, `modelName`, `promptVersion`, `invocationCount`, `latency`, `status`, `advisoryConfidence`, `riskLevel`.
- **Target in Sprint 7.3**: Add explicit `providerType` (`"LIVE"`, `"MOCK"`, `"NONE"`) to `AITelemetry`.

### 2.7 Golden Reference Regression Suite
- **Current State**: Tests are spread across several test classes.
- **Target in Sprint 7.3**: Consolidate and expand golden reference test cases in `DriverDispatchGoldenReferenceTest` covering:
  - Hard constraint enforcement (HOS, capacity, certifications, deadline).
  - Business rule incentives (Preferred driver, rest balance, regional affinity).
  - Deterministic multi-criteria scoring.
  - Single batched AI invocation invariant.
  - Safe fallback upon AI timeout or failure.
  - Resilient rejection of phantom/infeasible candidate recommendations.
  - Separation of deterministic factors and AI contextual insights in explainability.

---

## 3. Files Affected in Sprint 7.3

1. `backend/logistix-ai`:
   - `DispatchAIAdvice.java` [MODIFY: remove `suggestedScoreAdjustment`]
   - `DispatchPromptBuilder.java` [MODIFY: remove generic `candidateObj.toString()` overload]
   - `AITelemetry.java` [MODIFY: add `providerType`]
   - `SpringAIDispatchAIProvider.java` [MODIFY: enforce `DispatchAIRequest` prompt generation]
   - `MockDispatchAIProvider.java` [MODIFY: update `DispatchAIAdvice` instantiation]
   - `SpringAIDispatchAIProviderTest.java` [MODIFY]
2. `examples`:
   - `DispatchAIAdvisor.java` [DELETE: removed in favor of `MockDispatchAIProvider`]
   - `DriverDispatchAIStep.java` [MODIFY: validation of batched advice and telemetry]
   - `DriverDispatchRecommendationStep.java` [MODIFY]
   - `DriverDispatchReferenceApp.java` [MODIFY: canonical golden demo]
   - `DispatchDecisionPipelineFactory.java` [MODIFY]
   - `DriverDispatchGoldenReferenceTest.java` [NEW: comprehensive golden reference regression suite]
   - Existing test suites updated
3. `docs/`:
   - `docs/capabilities/driver-dispatch.md` [MODIFY]
   - `docs/sprints/sprint-7-3-baseline.md` [NEW]
   - `docs/sprints/sprint-7-3-completion.md` [NEW]
   - `architecture/ARCHITECTURE.md` [MODIFY]
   - `README.md` [MODIFY]

---

## 4. Verification Check
Running `mvn clean test` to confirm clean starting state before making changes.
