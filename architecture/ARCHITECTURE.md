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
| `logistix-domain` | Core domain entities (DecisionContext, Fact, Rule, Score, Recommendation, Explanation), Domain Events, and SPIs (`RuleProvider`, `ConstraintChecker`, `AIProvider`, `KnowledgeProvider`). Strictly framework-agnostic. | Java 21, Records |
| `logistix-model` | Declarative Decision Graph representations, node definitions, Directed Acyclic Graph (DAG) validation, topological sorting, and cycle detection. | JGraphT, Java 21 |
| `logistix-engine` | Synchronous and asynchronous pipeline execution runtime (`DecisionPipeline`, `PipelineStep`, `StepResult`, `DecisionExecutor`). | Virtual Threads (Java 21) |
| `logistix-dsl` | Fluent, type-safe Java Builder DSL for assembling decision graphs, registering steps, rules, and scoring policies. | Java 21 Fluent API |
| `logistix-ai` | Production-grade AI decision boundary, Spring AI adapters, structured prompt builders, batched candidate analysis, deterministic Mock AI test doubles, and typed `AITelemetry`. | Spring AI, Jackson |
| `logistix-rag` | Retrieval-Augmented Generation context providers, in-memory knowledge retrieval, and typed `KnowledgeTelemetry`. | Java 21 |
| `logistix-simulation` | Scenario generation, batch simulation, deterministic playback, and disruption modeling. | Java 21 |
| `logistix-benchmark` | High-throughput JMH benchmarks and micro-benchmarking harnesses. | JMH |
| `logistix-spring-boot-starter` | Spring Boot 3 auto-configuration, SPI bean discovery, condition evaluators, and lifecycle management. | Spring Boot 3.3.x |
| `logistix-api` | Enterprise REST endpoints, OpenAPI documentation, and problem-detail error handling. | Spring MVC, Springdoc |
| `logistix-examples` | Golden Reference Capabilities (Commercial Driver Dispatch, Decision Lab comparison engine, Scenario suite). | Java 21, Spring Boot 3 |

---

## 3. Hexagonal / Clean Architecture Boundaries

LogistiX adheres to Hexagonal Architecture principles:
- **Core Domain Isolation**: `logistix-domain` contains zero dependencies on external frameworks (Spring, Spring AI, JPA, Vector DBs).
- **Port/SPI Interfaces**: Ports (`AIProvider`, `KnowledgeProvider`, `RuleProvider`, `ConstraintChecker`) define abstract contracts.
- **Adapters**: Concrete implementations (`SpringAIDispatchAIProvider`, `MockDispatchAIProvider`, `InMemoryKnowledgeProvider`) implement ports in peripheral modules (`logistix-ai`, `logistix-rag`, `logistix-examples`).

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
    participant RAG as KnowledgeStep
    participant AI as AIStep
    participant Rec as RecommendationStep

    App->>Exec: execute(pipeline, initialContext)
    Exec->>Feas: execute(context)
    Feas-->>Exec: StepResult (feasible candidates filtered)
    Exec->>Rules: execute(context)
    Rules-->>Exec: StepResult (soft rules & incentives applied)
    Exec->>Score: execute(context)
    Score-->>Exec: StepResult (candidates mathematically ranked)
    opt KNOWLEDGE_AWARE Mode
        Exec->>RAG: execute(context) [Grounding Retrieval]
        RAG-->>Exec: StepResult (evidence retrieved & untrusted telemetry)
    end
    opt HYBRID_AI Mode
        Exec->>AI: execute(context) [1 Batched Call]
        AI-->>Exec: StepResult (contextual risk analysis & telemetry)
    end
    Exec->>Rec: execute(context)
    Rec-->>Exec: StepResult (deterministic policy evaluation, recommendation & explainability)
    Exec-->>App: DecisionResult<T>
```

---

## 5. Production AI & Knowledge Decision Boundary

LogistiX enforces a strict, production-hardened AI decision boundary:

```mermaid
flowchart TD
    FeasibleCandidates["Top-N Feasible Candidates (HARD-Validated)"] --> PromptBuilder["DispatchPromptBuilder<br/>(4 Structured Sections, Untrusted Data Warnings)"]
    KnowledgeDocs["Retrieved Evidence (Untrusted Reference Data)"] --> PromptBuilder
    PromptBuilder --> LLM["Spring AI / Configurable Mock AI<br/>(Single Batched Invocation)"]
    LLM --> SchemaValidator["Schema Validator & Evidence Citation Filter"]
    
    subgraph SafetyGuardrail ["LogistiX Boundary Guardrails"]
        SchemaValidator -- Valid Advice --> Telemetry["AITelemetry & KnowledgeTelemetry Recorder"]
        SchemaValidator -- "Timeout / Parsing Error / Rogue IDs" --> Fallback["Graceful Fallback Handler<br/>(Deterministic Rules Sole Decider)"]
    end
    
    Telemetry --> Policy["Deterministic Policy Evaluator"]
    Fallback --> Policy
    Policy --> FinalRec["Final Recommendation & Assignment"]
    FinalRec --> Explain["Auditable Explainability<br/>([DETERMINISTIC FACTORS] vs [KNOWLEDGE EVIDENCE] vs [AI CONTEXT])"]
```

---

## 6. Golden Reference Capability: Driver Dispatch

The **AI-Assisted Commercial Driver Dispatch Reference Capability** (`logistix-examples`) is the designated **Golden Reference Implementation** for LogistiX. It exemplifies:
- **Clean Architecture Separation**: `logistix-domain` contains 0 dependencies on Spring or Spring AI; AI is bridged strictly via the `AIProvider` SPI.
- **Inviolable Invariant**: *"The deterministic engine establishes what is feasible. Knowledge provides evidence. AI interprets evidence. A deterministic policy evaluates those signals. LogistiX retains authority over the final decision."*
- **Regression Standard**: Validated through `DriverDispatchGoldenReferenceTest`, `DriverDispatchDecisionLabTest`, and `KnowledgeGroundingBoundaryTest`.

---

## 7. Driver Dispatch Decision Lab (Sprint 8 & 8.1)

The **Driver Dispatch Decision Lab** (`org.logistix.examples.dispatch.lab`) provides a repeatable comparative framework that benchmarks `RULES_ONLY` vs `HYBRID_AI` on identical operational inputs.

```mermaid
flowchart TD
    Scenario["DispatchScenario (Immutable Input)"] --> Input["DispatchComparisonInput<br/>(Guaranteed Same FactBag & Context)"]
    Input --> Engine["DispatchComparisonEngine"]
    
    subgraph Execution ["Side-by-Side Pipelines"]
        Engine --> Rules["RULES_ONLY Pipeline<br/>(AI Calls: 0, Knowledge Calls: 0)"]
        Engine --> Hybrid["KNOWLEDGE_AWARE HYBRID_AI Pipeline<br/>(AI Calls: 1, Knowledge Retrieval: 1)"]
    end
    
    Rules --> Res1["Deterministic DecisionResult"]
    Hybrid --> Res2["Augmented DecisionResult"]
    
    Res1 & Res2 --> Comp["DispatchComparisonResult<br/>(Delta, Telemetry, Safety Verification)"]
    Comp --> Rep1["Scenario Summary Table"]
    Comp --> Rep2["Terminal Box Reporter (1080p Ready)"]
    Comp --> Rep3["Structured JSON Reporter"]
```

---

## 8. Knowledge Grounding & Test Double Architecture (Sprint 9, 9.1, 9.1.1)

1. **Deterministic Test Double (`MockDispatchAIProvider`)**:
   - Acts strictly as a configurable test double without implementing weather heuristics, driver rating analysis, or knowledge document parsing.
   - Test setups and scenarios explicitly configure candidate advisories. Unconfigured queries return safe, neutral advisories.
2. **Untrusted Reference Data Boundary**:
   - Retrieved enterprise documents are treated as untrusted reference data. System instructions explicitly prohibit executing embedded instructions or modifying hard constraints.
3. **Evidence Citation Validation**:
   - AI may only cite evidence provided in the request. Hallucinated, unknown, or duplicate IDs are stripped and normalized.
4. **Independent Telemetry & Explainability**:
   - `KnowledgeTelemetry` is segregated from `AITelemetry`.
   - Explainability distinctly separates `[DETERMINISTIC FACTORS]`, `[KNOWLEDGE EVIDENCE]`, and `[AI CONTEXTUAL INSIGHTS]`.

---

## 9. Governed AI Actions & MCP Execution Boundary (Sprint 10)

LogistiX enforces a strict, technology-neutral governed action architecture where external intelligence models can only propose actions, never execute them directly.

```
+------------------+       +-------------------------------+       +--------------------+       +----------------------+
|  AI / Decision   | ----> |      LogistiX Governance      | ----> |  AuthorizedAction  | ----> |   ActionExecutor     |
|     Proposal     |       | (Policies, Constraints, Risk) |       |  (Tokenized Grant) |       | (e.g. McpActionExec) |
+------------------+       +-------------------------------+       +--------------------+       +----------------------+
                                      |            |                                                        |
                            [REJECTED]|            |[APPROVAL_REQUIRED]                                     v
                                      v            v                                              +--------------------+
                                 +--------------------+                                           |   Enterprise Tool  |
                                 |  Audit Log Record  |                                           | (TMS / Fleet / DB) |
                                 |   (0 MCP Calls)    |                                           +--------------------+
                                 +--------------------+
```

### Architectural Principles:
1. **AI Proposes, LogistiX Decides**: AI proposals (`ActionProposal`) have zero direct authority and cannot be accepted by execution adapters.
2. **Deterministic Governance (`ActionGovernanceEngine`)**: Evaluates policies, hard constraints, and risk levels deterministically, returning `APPROVED`, `REJECTED`, or `APPROVAL_REQUIRED`.
3. **Authorized Action Invariant (`AuthorizedAction`)**: Only tokenized, validated `AuthorizedAction` instances can be executed by outbound adapters (`ActionExecutor`).
4. **Controlled Tool Registry (`ToolRegistry`)**: Outbound adapters execute only registered, pre-approved enterprise tools (`changeDeliveryAppointment`, `assignDriver`, `updateShipmentStatus`). Arbitrary tool invocations are rejected.
5. **Technology-Neutral Domain**: The core domain (`logistix-domain`) is 100% agnostic and unaware of MCP, HTTP, JSON-RPC, or external tool protocols. The Model Context Protocol exists purely as an infrastructure adapter (`logistix-mcp`).
6. **Segregated Telemetry & Complete Audit**: `ActionTelemetry` captures governance and execution latencies independently from AI/Knowledge telemetry. Every proposal, decision, and execution is recorded immutably in `ActionAuditEntry`.

---

## 10. Single Authority Registry & MCP Boundary Unification (Sprint 10.2.4)

The application context enforces a strict **Single Authority Registry Invariant**:
- There is exactly **one** `AuthorizationAuthorityRegistry` per LogistiX application context, owned and configured exclusively by core security (`logistix-spring-boot-starter`).
- The `logistix-mcp` module is an optional execution adapter that consumes the core `AuthorizationAuthorityRegistry` via `@ConditionalOnBean(AuthorizationAuthorityRegistry.class)` and `@AutoConfigureAfter(LogistiXAutoConfiguration.class)`.
- MCP cannot define or duplicate authorities, nor can it activate independently when security is disabled (`logistix.security.enabled=false`).


