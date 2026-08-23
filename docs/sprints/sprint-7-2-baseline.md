# Sprint 7.2 Baseline Report: Production-Grade AI Decision Boundary

## 1. Executive Summary
This document establishes the architecture baseline for **Sprint 7.2: Production-Grade AI Decision Boundary**.
Following the initial Spring AI integration in Sprint 7.1 (commit `5ee3f59`), this sprint hardens the AI interaction boundary to make it cost-efficient (single batched LLM call instead of multiple redundant calls), robust (explicit structured request DTO instead of `candidate.toString()`), deterministic in scoring policy (removing LLM direct score authority), strictly guarded against constraint overrides, and transparently observable with typed telemetry.

---

## 2. Current Implementation Audit & Identified Issues

### 2.1 Multiple / Redundant LLM Invocations & Missing Candidate Context
- **Current Behavior**:
  - `DriverDispatchAIStep` iterates over top 3 candidates and executes `aiProvider.generateReasoning(context, candidate)` (3 LLM calls).
  - Then it executes a 4th separate call `aiProvider.infer(context, DispatchAIAdvice.class)`, in which the candidate context passed to the prompt builder is `null`.
  - In `DispatchPromptBuilder`, when candidate is null, it appends `"No candidate object provided."`.
- **Target in Sprint 7.2**:
  - Create a structured `DispatchAIRequest` representing the shipment and top-N (default: 3) feasible candidates.
  - Perform exactly **ONE** single structured Spring AI invocation per dispatch decision containing all candidate context.
  - Eliminate un-contextualized `null` candidate calls.

### 2.2 LLM Direct Scoring Authority & Schema Drift
- **Current Behavior**:
  - `DispatchAIAdvice` contains `suggestedScoreAdjustment` which allows an LLM to directly alter the final numerical score.
  - The system prompt advertises bounds `[-0.20, +0.20]` whereas the DTO enforces bounds `[-0.50, +0.50]` (contract drift).
- **Target in Sprint 7.2**:
  - AI outputs purely qualitative/advisory signals (`riskLevel`, `advisoryConfidence`, `reasoning`, `contributingFactors`, `warnings`).
  - Deprecate / remove direct score modification from the AI contract. LogistiX deterministic policy remains the sole authority on scoring.

### 2.3 Fragile String-Dumping & Prompt Engineering
- **Current Behavior**: `DispatchPromptBuilder` relies on `candidateObj.toString()` to serialize candidate data into the prompt.
- **Target in Sprint 7.2**:
  - Use structured, domain-neutral DTOs (`DispatchAIRequest`, `CandidatePromptContext`) to format explicit, deterministic prompts.
  - Introduce explicit prompt versioning (e.g. `DRIVER_DISPATCH_AI_PROMPT_V1`) recorded in telemetry.

### 2.4 Timeout and Model Configuration Semantics
- **Current Behavior**:
  - `logistix.ai.timeout` (default: 3s) is present in `LogistiXProperties` but not enforced across `ChatModel.call(...)`.
  - `logistix.ai.model` is passed as an informational string to `SpringAIDispatchAIProvider`, which might not match the actual model runtime of the underlying Spring AI `ChatModel`.
- **Target in Sprint 7.2**:
  - Enforce timeout boundary safely (e.g., via `CompletableFuture` or request execution boundary with deterministic fallback upon timeout).
  - Document and expose real provider/model metadata in AI telemetry.

### 2.5 Auto-Configuration & Silent Mock Fallback
- **Current Behavior**: If `logistix.ai.provider=spring-ai` but no `ChatModel` bean exists in application context, starter silently falls back to `MockDispatchAIProvider`.
- **Target in Sprint 7.2**:
  - In production, when `logistix.ai.provider=spring-ai` is configured and no model is available, expose explicit fallback status or fail-fast if mock fallback is disabled.
  - Support `logistix.ai.fallback-to-mock` configuration property.

### 2.6 AI Provider Consolidation
- **Current Behavior**: `DispatchAIAdvisor` (legacy simulator in `examples`), `MockDispatchAIProvider` (in `logistix-ai`), and `SpringAIDispatchAIProvider` (in `logistix-ai`) coexist with overlapping roles.
- **Target in Sprint 7.2**:
  - Standardize on `SpringAIDispatchAIProvider` (live) and `MockDispatchAIProvider` (test/offline).
  - Mark `DispatchAIAdvisor` as legacy / delegate to `MockDispatchAIProvider`.

### 2.7 Typed AI Telemetry & Observability
- **Current Behavior**: Scattered untyped facts (`aiEnrichmentStatus`, `aiProviderName`, `aiAdvisoryConfidence`, `aiRiskLevel`) in `DecisionContext`.
- **Target in Sprint 7.2**:
  - Create a cohesive `AITelemetry` / `AIEvaluationSummary` record capturing invocation count, latency, prompt version, provider, model, risk level, and advisory confidence.

---

## 3. Files Affected in Sprint 7.2

1. `backend/logistix-ai`:
   - `org/logistix/ai/dispatch/DispatchAIRequest.java` [NEW]
   - `org/logistix/ai/dispatch/CandidateEvaluationAdvice.java` / `DispatchAIAdvice.java` [MODIFY]
   - `org/logistix/ai/dispatch/DispatchPromptBuilder.java` [MODIFY]
   - `org/logistix/ai/dispatch/SpringAIDispatchAIProvider.java` [MODIFY]
   - `org/logistix/ai/dispatch/MockDispatchAIProvider.java` [MODIFY]
   - `org/logistix/ai/dispatch/AITelemetry.java` [NEW]
   - `src/test/java/org/logistix/ai/dispatch/SpringAIDispatchAIProviderTest.java` [MODIFY]
2. `backend/logistix-spring-boot-starter`:
   - `org/logistix/starter/autoconfig/LogistiXProperties.java` [MODIFY]
   - `org/logistix/starter/autoconfig/LogistiXAutoConfiguration.java` [MODIFY]
3. `examples`:
   - `org/logistix/examples/dispatch/ai/DriverDispatchAIStep.java` [MODIFY]
   - `org/logistix/examples/dispatch/ai/DispatchAIAdvisor.java` [MODIFY]
   - `org/logistix/examples/dispatch/recommendation/DriverDispatchRecommendationStep.java` [MODIFY]
   - `org/logistix/examples/dispatch/DriverDispatchReferenceApp.java` [MODIFY]
   - `org/logistix/examples/dispatch/simulation/DispatchBenchmarkRunner.java` [MODIFY]
   - Test suites in `examples/src/test/java/org/logistix/examples/dispatch/` [MODIFY/NEW]
4. `docs/`:
   - `docs/capabilities/driver-dispatch.md` [MODIFY]
   - `docs/sprints/sprint-7-2-completion.md` [NEW]
   - `architecture/ARCHITECTURE.md` [MODIFY]
   - `README.md` [MODIFY]

---

## 4. Verification Check
Running `mvn clean test` to establish clean baseline pass rate before starting code modifications.
