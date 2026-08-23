# LogistiX Architecture

## 1. System Architecture Diagram

```mermaid
flowchart TB
    subgraph Core ["LogistiX Core Engine"]
        API["logistix-api<br/>(REST / Event Endpoints)"]
        STARTER["logistix-spring-boot-starter<br/>(Auto-configuration & Discovery)"]
        ENGINE["logistix-engine<br/>(Execution Pipeline & Rule Orchestration)"]
        DSL["logistix-dsl<br/>(Fluent Java DSL)"]
        MODEL["logistix-model<br/>(Decision Graph, DAG Validation)"]
        DOMAIN["logistix-domain<br/>(Entities, Value Objects, Domain Events)"]
        COMMON["logistix-common<br/>(Shared Utils, Geo & Math)"]
    end

    subgraph Intelligence ["Decision Intelligence Layer"]
        AI["logistix-ai<br/>(Spring AI Adapters, Prompts & Telemetry)"]
        RAG["logistix-rag<br/>(Context Ingestion & Retrieval)"]
        SIM["logistix-simulation<br/>(Scenario Runner & Monte Carlo)"]
        BENCH["logistix-benchmark<br/>(JMH & Latency Profiling)"]
    end

    subgraph Solutions ["Reference Implementations"]
        EXAMPLES["logistix-examples<br/>(Commercial Driver Dispatch & Decision Lab)"]
    end

    API --> ENGINE
    STARTER --> ENGINE
    ENGINE --> MODEL
    ENGINE --> DOMAIN
    DSL --> ENGINE
    AI --> DOMAIN
    RAG --> DOMAIN
    SIM --> ENGINE
    BENCH --> ENGINE
    EXAMPLES --> DSL
    EXAMPLES --> AI
```

---

## 2. Module Responsibilities

| Module | Responsibility | Key Technologies |
| :--- | :--- | :--- |
| `logistix-common` | Low-level geometry (Haversine distance), math utilities, common enumerations, and validation primitives. | Java 21 |
| `logistix-domain` | Core domain entities (DecisionContext, Fact, Rule, Score, Recommendation, Explanation), Domain Events, and SPIs (`RuleProvider`, `ConstraintChecker`, `AIProvider`). Strictly framework-agnostic. | Java 21, Records |
| `logistix-model` | Declarative Decision Graph representations, node definitions, Directed Acyclic Graph (DAG) validation, topological sorting, and cycle detection. | JGraphT, Java 21 |
| `logistix-engine` | Synchronous and asynchronous pipeline execution runtime (`DecisionPipeline`, `PipelineStep`, `StepResult`, `DecisionExecutor`). | Virtual Threads (Java 21) |
| `logistix-dsl` | Fluent, type-safe Java Builder DSL for assembling decision graphs, registering steps, rules, and scoring policies. | Java 21 Fluent API |
| `logistix-ai` | Production-grade AI decision boundary, Spring AI adapters, structured prompt builders, batched candidate analysis, and typed `AITelemetry`. | Spring AI, Jackson |
| `logistix-rag` | Retrieval-Augmented Generation context providers and vector integration interfaces. | Java 21 |
| `logistix-simulation` | Scenario generation, batch simulation, deterministic playback, and disruption modeling. | Java 21 |
| `logistix-benchmark` | High-throughput JMH benchmarks and micro-benchmarking harnesses. | JMH |
| `logistix-spring-boot-starter` | Spring Boot 3 auto-configuration, SPI bean discovery, condition evaluators, and lifecycle management. | Spring Boot 3.3.x |
| `logistix-api` | Enterprise REST endpoints, OpenAPI documentation, and problem-detail error handling. | Spring MVC, Springdoc |
| `logistix-examples` | Golden Reference Capabilities (Commercial Driver Dispatch, Decision Lab comparison engine). | Java 21, Spring Boot 3 |

---

## 3. Hexagonal / Clean Architecture Boundaries

LogistiX adheres to Hexagonal Architecture principles:
- **Core Domain Isolation**: `logistix-domain` contains zero dependencies on external frameworks (Spring, Spring AI, JPA).
- **Port/SPI Interfaces**: Ports (`AIProvider`, `RuleProvider`, `ConstraintChecker`) define abstract contracts.
- **Adapters**: Concrete implementations (e.g. `SpringAIDispatchAIProvider`, `MockDispatchAIProvider`) implement ports in peripheral modules (`logistix-ai`, `logistix-examples`).

---

## 4. Pipeline Execution Model

Decision pipelines are defined as ordered, composable steps executed sequentially with deterministic state progression:

```mermaid
sequenceDiagram
    participant App as Reference App / Client
    participant Exec as DecisionExecutor
    participant Ctx as DecisionContext
    participant Feas as FeasibilityStep
    participant Rules as RuleEvaluationStep
    participant Score as MultiCriteriaScoringStep
    participant AI as AIStep
    participant Rec as RecommendationStep

    App->>Exec: execute(pipeline, initialContext)
    Exec->>Feas: execute(context)
    Feas-->>Exec: StepResult (feasible candidates filtered)
    Exec->>Rules: execute(context)
    Rules-->>Exec: StepResult (soft rules & incentives applied)
    Exec->>Score: execute(context)
    Score-->>Exec: StepResult (candidates mathematically ranked)
    opt HYBRID_AI Mode
        Exec->>AI: execute(context) [1 Batched Call]
        AI-->>Exec: StepResult (contextual risk analysis & telemetry)
    end
    Exec->>Rec: execute(context)
    Rec-->>Exec: StepResult (deterministic policy evaluation, recommendation & explainability)
    Exec-->>App: DecisionResult<T>
```

---

## 5. Production AI Decision Boundary

LogistiX enforces a strict, production-hardened AI decision boundary:

```mermaid
flowchart TD
    FeasibleCandidates["Top-N Feasible Candidates (HARD-Validated)"] --> PromptBuilder["DispatchPromptBuilder<br/>(Strict JSON Schema, Zero CoT Tokens)"]
    PromptBuilder --> LLM["Spring AI / Mock AI Provider<br/>(Single Batched Invocation)"]
    LLM --> SchemaValidator["Schema Validator & Candidate ID Checker"]
    
    subgraph SafetyGuardrail ["LogistiX Boundary Guardrails"]
        SchemaValidator -- Valid Advice --> Telemetry["AITelemetry Recorder<br/>(Tokens, Latency, Confidence, Risk)"]
        SchemaValidator -- "Timeout / Parsing Error / Rogue IDs" --> Fallback["Graceful Fallback Handler<br/>(Deterministic Rules Sole Decider)"]
    end
    
    Telemetry --> Policy["Deterministic Policy Evaluator"]
    Fallback --> Policy
    Policy --> FinalRec["Final Recommendation & Assignment"]
    FinalRec --> Explain["Auditable Explainability<br/>(Deterministic Factors vs. AI Context vs. AITelemetry)"]
```

---

## 6. Golden Reference Capability: Driver Dispatch

The **AI-Assisted Commercial Driver Dispatch Reference Capability** (`logistix-examples`) is the designated **Golden Reference Implementation** for LogistiX. It exemplifies:
- **Clean Architecture Separation**: `logistix-domain` contains 0 dependencies on Spring or Spring AI; AI is bridged strictly via the `AIProvider` SPI.
- **Inviolable Invariant**: *"The deterministic engine establishes what is feasible. AI provides contextual advisory signals. A deterministic policy evaluates those signals. LogistiX retains authority over the final decision."*
- **Regression Standard**: Validated through `DriverDispatchGoldenReferenceTest` and `DriverDispatchDecisionLabTest`.

---

## 7. Driver Dispatch Decision Lab (Sprint 8 & 8.1)

The **Driver Dispatch Decision Lab** (`org.logistix.examples.dispatch.lab`) provides a repeatable comparative framework that benchmarks `RULES_ONLY` vs `HYBRID_AI` on identical operational inputs.

```mermaid
flowchart TD
    Scenario["DispatchScenario (Immutable Input)"] --> Input["DispatchComparisonInput<br/>(Guaranteed Same FactBag & Context)"]
    Input --> Engine["DispatchComparisonEngine"]
    
    subgraph Execution ["Side-by-Side Pipelines"]
        Engine --> Rules["RULES_ONLY Pipeline<br/>(AI Calls: 0)"]
        Engine --> Hybrid["HYBRID_AI Pipeline<br/>(AI Calls: 1)"]
    end
    
    Rules --> Res1["Deterministic DecisionResult"]
    Hybrid --> Res2["Augmented DecisionResult"]
    
    Res1 & Res2 --> Comp["DispatchComparisonResult<br/>(Delta, Telemetry, Safety Verification)"]
    Comp --> Rep1["Scenario Summary Table"]
    Comp --> Rep2["Terminal Box Reporter (1080p Ready)"]
    Comp --> Rep3["Structured JSON Reporter"]
```

### Core Architectural Insights:
1. **Decision Integrity**: Deterministic feasibility constraints and multi-criteria scoring retain authority.
2. **Context Enrichment**: AI introduces qualitative environmental risk reasoning without manipulating numerical weights.
3. **Deterministic Policy Evaluation**: AI advisory signals are evaluated by a deterministic policy among already HARD-feasible candidates.
4. **Safety Guarantee**: Unsafe or uncertified candidates are filtered deterministically, ensuring that AI recommendations can never compromise operational safety.
5. **Benchmark Semantics**: Explicitly differentiates zero-AI JVM execution, in-memory Mock AI orchestration testing, and live LLM inference with AI overhead accounting.
