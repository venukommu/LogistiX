# LogistiX Architecture Specification: Framework Execution Engine Runtime

## 1. Executive Summary & Philosophy

**LogistiX** is an open-source, domain-agnostic framework for **AI-powered operational decision making**. 

Rather than being a static logistics application, LogistiX provides a resilient, extensible execution runtime inspired by leading framework architectures (Spring Framework, LangGraph, Temporal, Apache Camel).

Every operational decision in LogistiX—from driver dispatch to dynamic pricing, carrier recommendation, route optimization, dock scheduling, and fraud detection—executes via the **LogistiX Execution Engine (`logistix-engine`)**.

---

## 2. Core Framework Runtime Components

### A. `LogistiXContext` (The Global Container)
The runtime container analogous to Spring's `ApplicationContext`. It centralizes:
- **`DecisionRegistry`**: Dynamic registration and lookup of decision pipelines.
- **`PluginRegistry`**: Discovery and lifecycle management for third-party extensions.
- **`HookRegistry`**: Interceptor lifecycle hooks.
- **`MetricsCollector`**: Aggregates timing, rule evaluations, constraint violations, and AI token telemetry.
- **`DomainEventPublisher`**: Broadcasts lifecycle events asynchronously.
- **`DecisionExecutor`**: The main pipeline execution engine.

### B. `DecisionPipeline` & `DecisionStep`
- **`DecisionPipeline`**: Immutable sequential execution abstraction created via `DecisionPipelineBuilder`.
- **`DecisionStep`**: Pure transformation contract $( \text{DecisionContext} \to \text{StepResult} )$. Pipelines are agnostic to domain semantics.
- Specialized step contracts: `ConstraintStep`, `RuleStep`, `AIStep`, `ScoringStep`, `RecommendationStep`.

### C. `DecisionExecutor`
The core runtime coordinator:
1. Resolves pipelines via `DecisionRegistry`.
2. Triggers `BeforeDecision` / `BeforeStep` / `AfterStep` / `AfterDecision` lifecycle hooks.
3. Records `StepMetrics` and builds replayable `DecisionTrace` entries.
4. Handles short-circuiting or failure recovery according to `EngineConfiguration`.
5. Emits `DecisionCompletedEvent` and returns the final `DecisionResult`.

---

## 3. Decision Engine Runtime Flow

```mermaid
flowchart TD
    subgraph Client ["1. Invocation"]
        REQ["<b>DecisionRequest&lt;T&gt;</b>"]
    end

    subgraph Container ["2. LogistiXContext Runtime Container"]
        REG["<b>DecisionRegistry</b><br/><i>Locates Pipeline by DecisionType</i>"]
        HOOKS["<b>HookRegistry</b><br/><i>Lifecycle Interceptors</i>"]
        METRICS["<b>MetricsCollector</b><br/><i>Telemetry & Latency</i>"]
        TRACE["<b>TraceRecorder</b><br/><i>Replayable Audit Trail</i>"]
    end

    subgraph Pipeline ["3. DecisionPipeline Execution Flow (DecisionExecutor)"]
        direction TB
        H_BEFORE["<i>Hook: BeforeDecision</i>"]
        
        STEP1["<b>ConstraintStep</b><br/><i>Feasibility Pruning & Hard Guardrails</i>"]
        STEP2["<b>RuleStep</b><br/><i>Deterministic Business Policy Compliance</i>"]
        STEP3["<b>AIStep</b><br/><i>Semantic Reasoning & RAG Grounding</i>"]
        STEP4["<b>ScoringStep</b><br/><i>Multi-Criteria Weighted Evaluation</i>"]
        STEP5["<b>RecommendationStep</b><br/><i>Candidate Ranking & Explanation</i>"]
        
        H_AFTER["<i>Hook: AfterDecision</i>"]
        
        H_BEFORE --> STEP1
        STEP1 --> STEP2
        STEP2 --> STEP3
        STEP3 --> STEP4
        STEP4 --> STEP5
        STEP5 --> H_AFTER
    end

    subgraph Output ["4. Auditable Output"]
        RES["<b>DecisionResult&lt;T&gt;</b><br/>• Top Recommendation & Rank<br/>• Normalized Score & Confidence<br/>• Explanation & Factor Breakdown<br/>• DecisionMetrics & Replayable DecisionTrace<br/>• Audit Logs & Metadata"]
    end

    REQ --> REG
    REG --> Pipeline
    Pipeline --> METRICS
    Pipeline --> TRACE
    Pipeline --> RES
```

---

## 4. Plugin Architecture (`org.logistix.engine.plugins`)

LogistiX features a non-invasive plugin model allowing third-party extensions to dynamically contribute:
- Custom **`DecisionStep`** implementations.
- Custom **`DecisionHook`** lifecycle interceptors.
- External rules, constraints, and scoring providers.

```mermaid
graph LR
    P[<b>DecisionPlugin</b>] -->|Contributes| S[Custom DecisionSteps]
    P -->|Contributes| H[Lifecycle DecisionHooks]
    P -->|Initializes via| PC[PluginContext]
    PR[<b>PluginRegistry</b>] -->|Manages| P
    LC[<b>LogistiXContext</b>] -->|Hosts| PR
```

---

## 5. Decision Trace & Metrics Telemetry

### Replayable Trace (`DecisionTrace`)
Each executed step appends a `DecisionTraceEntry` capturing:
- Step identifier and human-readable name.
- Input and output state diffs.
- Emitted fact keys.
- Step execution status (`SUCCESS`, `SKIPPED`, `FAILED`, `SHORT_CIRCUIT`).
- Execution duration and messages.

This enables UI visualizers and simulation tools to **replay the entire decision lifecycle step-by-step**.

### Quantitative Metrics (`DecisionMetrics`)
- Total execution duration.
- Per-step duration breakdown.
- Evaluated vs. passed rule counts.
- Violated constraint counts.
- AI tokens consumed and AI model inference latency.
- Warning and error tallies.

---

## 6. Multi-Module Layout

```mermaid
graph TD
    api[logistix-api<br/><i>REST & OpenAPI Gateway</i>] --> starter[logistix-starter<br/><i>Auto-Configuration & Wiring</i>]
    starter --> engine[<b>logistix-engine</b><br/><i>Runtime Execution Engine</i>]
    starter --> rag[logistix-rag<br/><i>RAG & Knowledge Retrieval</i>]
    starter --> ai[logistix-ai<br/><i>Model Providers & Prompts</i>]
    starter --> sim[logistix-simulation<br/><i>Fleet & Weather Simulators</i>]
    starter --> bm[logistix-benchmark<br/><i>Model & Decision Evaluators</i>]
    
    engine --> domain[<b>logistix-domain</b><br/><i>Pure Java 21 Framework Core</i>]
    de[logistix-decision-engine] --> domain
    rag --> domain
    ai --> domain
    sim --> domain
    bm --> domain
    
    domain --> common[logistix-common<br/><i>Shared Models & Exceptions</i>]
```
