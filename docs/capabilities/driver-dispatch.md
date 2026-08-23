# Commercial Driver Dispatch: Golden Reference Capability

## 1. Capability Overview

The **Commercial Driver Dispatch** capability serves as the **Golden Reference Implementation** for the LogistiX Decision Intelligence framework. It demonstrates how autonomous operational decisions can combine deterministic safety constraints, multi-criteria mathematical scoring, and contextual AI reasoning without sacrificing regulatory compliance, determinism, or auditability.

```
"The deterministic engine establishes what is feasible.
 AI provides contextual advisory signals.
 A deterministic policy evaluates those signals.
 LogistiX retains authority over the final decision."
```

---

## 2. Decision Pipeline Topology

The dispatch decision operates over a 5-stage declarative pipeline executed via the `logistix-engine` and `logistix-dsl`:

```
┌─────────────────────────┐
│ 1. Driver Feasibility   │  HARD Constraints: HOS (Hours-of-Service), Payload Weight & Volume,
│    Filter Step          │  Mandatory Certifications (HazMat, Tanker, TWIC), Delivery Deadline SLA.
└───────────┬─────────────┘
            ▼
┌─────────────────────────┐
│ 2. Business Rule        │  SOFT Constraints & Policy Adjustments: Preferred carrier bonuses,
│    Evaluation Step      │  driver tier incentives, rest-period optimizations.
└───────────┬─────────────┘
            ▼
┌─────────────────────────┐
│ 3. Multi-Criteria       │  Deterministic Scoring: Weighted composite score computed across
│    Scoring Step         │  deadhead distance, SLA buffer, driver rating, and trip economics.
└───────────┬─────────────┘
            ▼
┌─────────────────────────┐
│ 4. AI Contextual        │  Contextual Risk Advisory: Evaluates feasible candidates in a single
│    Advisor Step (AI)    │  batched LLM call for weather, corridor anomalies, and driver readiness.
└───────────┬─────────────┘
            ▼
┌─────────────────────────┐
│ 5. Recommendation &     │  Deterministic Policy Selection & Explainability: Evaluates AI risk signals
│    Explainability Step  │  for top feasible candidates and synthesizes full feature contributions.
└─────────────────────────┘
```

---

## 3. Key Operational Constraints

### Hard Constraints (Non-Negotiable)
- **Hours-of-Service (HOS)**: The driver must possess sufficient remaining driving hours to cover both deadhead distance and primary linehaul distance with mandatory safety margins.
- **Payload Capacity**: The shipment's weight (kg) and volume ($m^3$) must not exceed the vehicle's certified maximum payload.
- **Regulatory Certifications**: The driver must hold all required endorsements (e.g. `HAZMAT`, `TWIC`, `REEFER`). Missing any required certification results in immediate deterministic disqualification.
- **Delivery Deadline SLA**: Estimated Time of Arrival (ETA) must precede the strict shipment delivery deadline.

### Soft Constraints & Scoring Features
- **Deadhead Proximity** (Weight: 25%): Proximity to origin minimizing empty-miles cost.
- **ETA SLA Margin** (Weight: 25%): Time buffer before delivery deadline.
- **Driver Performance & History** (Weight: 20%): Historical on-time delivery rate and driver rating.
- **Cost Efficiency** (Weight: 15%): Total trip cost including deadhead fuel and driver per-mile rate.
- **Business Rule Incentives** (Weight: 15%): Driver loyalty tier (`PLATINUM`, `GOLD`, `SILVER`, `STANDARD`).

---

## 4. Architectural Invariants

1. **Zero AI Invocations in `RULES_ONLY` Mode**: The deterministic rules pipeline executes entirely in-memory with zero AI calls and sub-millisecond latency.
2. **Single Batched AI Invocation in `HYBRID_AI` Mode**: Feasible candidates are formatted into a single structured prompt evaluated in exactly ONE LLM call.
3. **Hard Constraint Inviolability**: The AI cannot override hard constraints or recommend infeasible drivers.
4. **Deterministic Scoring Authority**: Mathematical scoring is exclusively calculated by the deterministic scoring engine. AI provides qualitative risk signals (`LOW`, `MEDIUM`, `HIGH`, `CRITICAL`) and operational insights.
5. **Deterministic Policy Evaluation**: If AI identifies elevated corridor risk on the initial leader, the deterministic selection policy may select a close, fully certified top-tier runner-up with documented operational reasoning.
6. **Graceful Fallback**: If the AI model times out or is offline, the pipeline falls back to deterministic rules with zero downtime.

---

## 5. Driver Dispatch Decision Lab (Sprint 8 & 8.1)

The **Driver Dispatch Decision Lab** (`org.logistix.examples.dispatch.lab`) provides a side-by-side comparison engine measuring the exact impact of AI decision augmentation on identical operational scenarios.

### Golden Demonstration Scenarios:
1. **`baseline-clear` (Scenario 1: AI Confirms)**: Clear corridor where AI confirms the deterministic choice while providing operational reassurance.
2. **`corridor-weather-risk` (Scenario 2: AI Adds Context)**: Moderate rain slowdown where AI adds operational delay telemetry while confirming the top candidate.
3. **`safety-constraint-protection` (Scenario 3: Safety Guardrail)**: Infeasible candidates (missing certifications, HOS exhaustion) are rejected by deterministic guardrails, proving that AI cannot override hard constraints.
4. **`ai-contextual-decision` (Scenario 4: AI Contextual Differentiation)**: Severe mountain blizzard on Donner Pass. Both candidates pass hard constraints. `RULES_ONLY` selects Driver A on proximity; `HYBRID_AI` identifies severe blizzard risk on Driver A, leading the deterministic policy to safely select Driver B (Platinum winter veteran).

---

## 6. Screen-Recording Demonstration Commands

```bash
# Demo 1: Deterministic Rules Only (0 AI calls)
mvn exec:java -Dexec.mainClass="org.logistix.examples.dispatch.DriverDispatchReferenceApp" \
  -Dexec.args="--mode rules-only" -f backend/pom.xml -pl :logistix-examples

# Demo 2: Hybrid AI Reference Demo (1 batched AI call, telemetry & explainability)
mvn exec:java -Dexec.mainClass="org.logistix.examples.dispatch.DriverDispatchReferenceApp" \
  -Dexec.args="--mode hybrid" -f backend/pom.xml -pl :logistix-examples

# Demo 3: Side-by-Side Comparison on AI Contextual Decision Scenario
mvn exec:java -Dexec.mainClass="org.logistix.examples.dispatch.DriverDispatchReferenceApp" \
  -Dexec.args="--compare --scenario ai-contextual-decision" -f backend/pom.xml -pl :logistix-examples

# Demo 4: Full Decision Lab Suite with Executive Summary Table
mvn exec:java -Dexec.mainClass="org.logistix.examples.dispatch.DriverDispatchReferenceApp" \
  -Dexec.args="--compare --scenario all" -f backend/pom.xml -pl :logistix-examples

# Demo 5: Machine-Readable JSON Export
mvn exec:java -Dexec.mainClass="org.logistix.examples.dispatch.DriverDispatchReferenceApp" \
  -Dexec.args="--compare --scenario ai-contextual-decision --format json" -f backend/pom.xml -pl :logistix-examples
```

---

## 7. Benchmark Semantics & Interpretation

The Decision Lab benchmark clearly distinguishes:
- **`RULES_ONLY` (Deterministic JVM Execution)**: 0 AI calls, ~1,200+ ops/sec, sub-millisecond latency. Baseline throughput of the core decision engine.
- **`HYBRID_MOCK` (In-memory Mock AI)**: 1 AI call, ~1,500+ ops/sec. Validates pipeline orchestration and decision boundaries without measuring LLM latency.
- **`HYBRID_LIVE` (Live Spring AI Model)**: 1 AI call. Measures real LLM network and inference latency with explicit **AI Overhead** calculation (`Total Hybrid Latency - Rules Only Latency`).
