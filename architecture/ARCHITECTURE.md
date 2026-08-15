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
