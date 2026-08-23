# LogistiX Architecture Specification: Release Candidate 1 (RC1)

## 1. Executive Summary & Philosophy

**LogistiX** is an open-source, domain-agnostic **Decision Intelligence Platform**.

Rather than treating operational decisions as rigid sequential scripts, LogistiX decouples **Decision Modeling** (describing *what* needs to be evaluated) from **Execution Strategy** (determining *how*, when, and in what topology computation happens).

Every decision problem—whether Driver Dispatch, Carrier Recommendation, Multi-Stop Route Optimization, Dynamic Pricing, Dock Scheduling, or Multi-Agent Negotiation—is modeled as a **`DecisionModel`** executed through pluggable execution strategies.

---

## 2. Decision Intelligence Architecture (`logistix-model` & `logistix-engine`)

```mermaid
flowchart TD
    subgraph Client ["Client Invocation"]
        REQ["<b>DecisionRequest&lt;T&gt;</b> or <b>LogistiX.decision()</b>"]
    end

    subgraph ModelLayer ["Decision Model Layer (logistix-model)"]
        DM["<b>DecisionModel</b><br/><i>(Declarative Topology Description)</i>"]
        
        subgraph Topologies ["Supported Model Topologies"]
            DG["<b>DecisionGraph</b><br/><i>(DAG, Cyclic, Branching)</i>"]
            DP["<b>ModelPipeline</b><br/><i>(Sequential Pipeline)</i>"]
        end
        
        DM --> DG
        DM --> DP
    end

    subgraph StrategyLayer ["Execution Strategies (ExecutionStrategy)"]
        S_SEQ["<b>SequentialExecutionStrategy</b>"]
        S_PAR["<b>ParallelExecutionStrategy</b>"]
        S_GRA["<b>GraphExecutionStrategy</b><br/><i>(Topological DAG Sorting)</i>"]
        S_CON["<b>ConditionalExecutionStrategy</b><br/><i>(Dynamic Edge Branching)</i>"]
        S_AGE["<b>AgentExecutionStrategy</b><br/><i>(ReAct & Multi-Agent Loops)</i>"]
    end

    subgraph Planning ["Execution Planning"]
        PLAN["<b>ExecutionPlan</b><br/>• ExecutionStages<br/>• ExecutionUnits<br/>• ExecutionCursor"]
    end

    subgraph Runtime ["Execution Engine & Telemetry (logistix-engine)"]
        STATE["<b>DecisionState</b><br/><i>(Facts, NodeOutputs, Errors, Variables)</i>"]
        MEM["<b>DecisionMemory</b><br/><i>(Remember, Retrieve, Search)</i>"]
        VIS["<b>DecisionVisualizer</b><br/><i>(Mermaid, JSON, PlantUML, GraphViz)</i>"]
        EXEC["<b>DecisionExecutor</b>"]
        RES["<b>DecisionResult&lt;T&gt;</b>"]
    end

    REQ --> DM
    DM --> StrategyLayer
    StrategyLayer --> PLAN
    PLAN --> EXEC
    EXEC --> STATE
    EXEC --> MEM
    EXEC --> RES
    DM --> VIS
```

---

## 3. Pluggable Decision Node Types (`org.logistix.model.node`)

Each node in a `DecisionModel` represents an atomic, isolated unit of work:

| Node Type | Class Contract | Responsibility |
| :--- | :--- | :--- |
| **Constraint** | `ConstraintNode` | Evaluates hard feasibility guardrails and soft boundaries. |
| **Rule** | `RuleNode` | Evaluates deterministic business compliance with priority. |
| **AI / LLM** | `AINode` | Model inference, contextual reasoning, and prompt execution. |
| **Memory** | `MemoryNode` | Short-term working context and long-term historical recall. |
| **Scoring** | `ScoringNode` | Computes normalized multi-criteria objective scores. |
| **Recommendation** | `RecommendationNode` | Synthesizes top-K candidates with explainable rationale. |
| **Validation** | `ValidationNode` | Verifies fact schema completeness and invariant checks. |
| **Transformation** | `TransformationNode` | Data shaping, schema mapping, and derived state computation. |
| **Aggregation** | `AggregationNode` | Merges outputs from multiple concurrent upstream branches. |
| **Condition** | `ConditionNode` | Evaluates dynamic boolean expressions for branch routing. |
| **Delay** | `DelayNode` | Execution throttling, retry backoff, and deliberate delays. |

---

## 4. Consolidated Multi-Module Hierarchy

```mermaid
graph TD
    api[logistix-api<br/><i>REST Gateway & OpenAPI</i>] --> starter[<b>logistix-spring-boot-starter</b><br/><i>Spring AutoConfiguration</i>]
    starter --> dsl[<b>logistix-dsl</b><br/><i>Public API & Fluent DSL</i>]
    examples[logistix-examples<br/><i>Code Samples & Guides</i>] --> dsl
    
    dsl --> engine[<b>logistix-engine</b><br/><i>Runtime Execution Engine</i>]
    dsl --> model[<b>logistix-model</b><br/><i>Decision Modeling & Graphs</i>]
    engine --> model
    
    engine --> domain[<b>logistix-domain</b><br/><i>Pure Framework Domain Core</i>]
    model --> domain
    
    starter --> rag[logistix-rag<br/><i>RAG & Knowledge Retrieval</i>]
    starter --> ai[logistix-ai<br/><i>Model Providers & Prompts</i>]
    starter --> sim[logistix-simulation<br/><i>Fleet & Weather Simulators</i>]
    starter --> bm[logistix-benchmark<br/><i>Model & Decision Evaluators</i>]
    
    rag --> domain
    ai --> domain
    sim --> domain
    bm --> domain
    
    domain --> common[logistix-common<br/><i>Shared Models & Utilities</i>]
```

---

## 5. API Stability Matrix (RC1)

| Contract Group | Package | Stability |
| :--- | :--- | :--- |
| **Public Entry Point** | `org.logistix.dsl.LogistiX` | **Stable** |
| **Fluent DSLs** | `org.logistix.dsl.fluent.*` | **Stable** |
| **Annotations** | `org.logistix.dsl.annotation.*` | **Stable** |
| **Core Domain Models** | `org.logistix.domain.decision.*`, `org.logistix.domain.fact.*` | **Stable** |
| **Decision Models & Graph** | `org.logistix.model.model.*`, `org.logistix.model.graph.*` | **Stable** |
| **Execution Strategies** | `org.logistix.model.strategy.*` | **Stable** |
| **Engine Runtime & SPI** | `org.logistix.engine.executor.*`, `org.logistix.engine.plugins.*` | **Stable** |
| **Spring Boot Integration** | `org.logistix.starter.autoconfig.*`, `org.logistix.starter.scanner.*` | **Stable** |

---

## 6. Hexagonal AI Integration Architecture (`logistix-ai`)

LogistiX adheres to Hexagonal Architecture for all AI/LLM integrations. The generic core domain defines the outbound SPI (`AIProvider`), while adapter implementations reside exclusively in `logistix-ai` and `logistix-spring-boot-starter`.

```mermaid
flowchart TD
    subgraph Core ["LogistiX Core Engine"]
        Context["DecisionContext"]
        HardConstraints["Deterministic Feasibility Pruning"]
        Scoring["Deterministic Scoring Engine"]
    end

    subgraph Port ["Outbound Port (logistix-domain)"]
        SPI["AIProvider SPI"]
    end

    subgraph Adapter ["Infrastructure Adapters (logistix-ai)"]
        SpringAI["SpringAIDispatchAIProvider"]
        MockAI["MockDispatchAIProvider"]
        PromptBuilder["DispatchPromptBuilder"]
        DTO["DispatchAIAdvice DTO"]
    end

    subgraph LLM ["Model Providers"]
        Ollama["Local Models (Ollama: llama3.2, mistral)"]
        Cloud["Cloud Models (OpenAI, Claude, Gemini)"]
    end

    Core --> HardConstraints
    HardConstraints -- Feasible Candidates Only --> Scoring
    Scoring --> SPI
    SPI -. Implemented by .-> SpringAI
    SPI -. Implemented by .-> MockAI
    SpringAI --> PromptBuilder
    SpringAI --> DTO
    SpringAI --> Ollama & Cloud
```

### Inviolable AI Principles
1. **Advisory Role**: AI provides qualitative reasoning, contextual risk assessment, and advisory confidence.
2. **Inviolable Constraints**: AI **cannot** override hard operational constraints or resurrect unfeasible candidates.
3. **Resilient Fallback**: Model timeouts or network exceptions trigger instant, zero-downtime fallback to deterministic rules.
4. **Single-Call Batched Invocation**: Feasible candidates are evaluated collectively in one structured LLM call (`DispatchAIRequest`), eliminating redundant API round-trips.
5. **No AI Direct Score Authority**: LogistiX deterministic policy retains sole authority over final candidate scoring and selection.

---

## 7. Production-Grade AI Decision Boundary (Sprint 7.2 & 7.3)

```mermaid
flowchart TD
    AllCandidates["All Candidates Fleet"] --> Constraints["Deterministic Hard Constraints<br/>(HOS, Weight/Volume, Certs, Deadlines)"]
    Constraints -- Infeasible Rejected --> Pruned["Pruned Set"]
    Constraints -- Feasible Candidates Only --> Rules["Deterministic Business Rules<br/>(Tiers, Rest Balance, Regional Affinity)"]
    Rules --> Scoring["Multi-Criteria Scoring Engine<br/>(Proximity, SLA Margin, Cost, Reliability)"]
    Scoring --> TopN["Top-N Feasible Candidate Selector<br/>(Default: topN = 3)"]
    TopN --> AIReq["Single Batched Request DTO<br/>(DispatchAIRequest + CandidatePromptContext)"]
    AIReq --> LLM["Spring AI ChatModel (Single Call)<br/>(llama3.2 / GPT-4o / Claude 3.5)"]
    LLM --> StructuredDTO["Validated Batched Advice DTO<br/>(RiskLevel, AdvisoryConfidence, Reasoning)"]
    StructuredDTO --> Policy["Deterministic Decision & Selection Policy"]
    Policy --> FinalRec["Final Recommendation & Assignment"]
    FinalRec --> Explain["Auditable Explainability<br/>(Deterministic Factors vs. AI Context vs. AITelemetry)"]
```

---

## 8. Golden Reference Capability: Driver Dispatch (Sprint 7.x Closure)

The **AI-Assisted Commercial Driver Dispatch Reference Capability** (`logistix-examples`) is the designated **Golden Reference Implementation** for LogistiX. It exemplifies:
- **Clean Architecture Separation**: `logistix-domain` contains 0 dependencies on Spring or Spring AI; AI is bridged strictly via the `AIProvider` SPI.
- **Inviolable Invariant**: "The AI can reason. LogistiX decides."
- **Regression Standard**: Validated through `DriverDispatchGoldenReferenceTest` covering constraints, rules, scoring, single-call invocation invariant, fail-safe degradation, and explainability feature attribution.



