# LogistiX Architecture Specification: Decision Intelligence Platform

## 1. Executive Summary & Philosophy

**LogistiX** is an open-source, domain-agnostic **Decision Intelligence Platform**.

Rather than treating operational decisions as rigid sequential scripts, LogistiX decouples **Decision Modeling** (describing *what* needs to be evaluated) from **Execution Strategy** (determining *how*, when, and in what topology computation happens).

Every decision problem—whether Driver Dispatch, Carrier Recommendation, Multi-Stop Route Optimization, Dynamic Pricing, Dock Scheduling, or Multi-Agent Negotiation—is modeled as a **`DecisionModel`** executed through pluggable execution strategies.

---

## 2. Decision Intelligence Architecture (`logistix-model`)

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
            DT["<b>DecisionTable / DecisionTree</b>"]
        end
        
        DM --> DG
        DM --> DP
        DM --> DT
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

    subgraph Runtime ["Execution Engine & Telemetry"]
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

## 4. Decision Graph & Fluent Graph DSL

```java
// Construct a DAG DecisionGraph with zero boilerplate
DecisionGraph graph = LogistiX.graph("carrier-intelligence-graph")
    .name("Carrier Multi-Branch Evaluation")
    .addNode(DecisionGraphNode.of("hos-check", "Check Driver HOS", NodeType.CONSTRAINT))
    .addNode(DecisionGraphNode.of("weather-risk", "Predict Weather Impact", NodeType.AI))
    .addNode(DecisionGraphNode.of("traffic-delay", "Predict Traffic Congestion", NodeType.AI))
    .addNode(DecisionGraphNode.of("score-synthesis", "Aggregate & Score", NodeType.SCORING, List.of("weather-risk", "traffic-delay")))
    .addNode(DecisionGraphNode.of("recommendation", "Final Recommendation", NodeType.RECOMMENDATION, List.of("score-synthesis")))
    .addEdge("hos-check", "weather-risk")
    .addEdge("hos-check", "traffic-delay")
    .addEdge("weather-risk", "score-synthesis")
    .addEdge("traffic-delay", "score-synthesis")
    .addEdge("score-synthesis", "recommendation")
    .build();

// Render to Mermaid
String mermaidDiagram = LogistiX.visualizer().toMermaid(graph);
```

---

## 5. Declarative YAML DSL Schema

Decision topologies can be authored directly in YAML for zero-code deployments:

```yaml
decision:
  name: dynamic-driver-dispatch
  strategy: graph
  version: 1.0.0
  description: Multi-branch AI dispatch under real-time constraints
  nodes:
    - id: hos-guardrail
      type: CONSTRAINT
    - id: weather-inference
      type: AI
      properties:
        model: gpt-4o
        temperature: 0.2
    - id: congestion-model
      type: AI
      properties:
        model: gpt-4o
    - id: multi-criteria-scorer
      type: SCORING
      dependencies: [weather-inference, congestion-model]
    - id: dispatch-recommendation
      type: RECOMMENDATION
      dependencies: [multi-criteria-scorer]
  edges:
    - source: hos-guardrail
      target: weather-inference
    - source: hos-guardrail
      target: congestion-model
    - source: weather-inference
      target: multi-criteria-scorer
    - source: congestion-model
      target: multi-criteria-scorer
    - source: multi-criteria-scorer
      target: dispatch-recommendation
```

---

## 6. Execution Strategies (`org.logistix.model.strategy`)

1. **`SequentialExecutionStrategy`**: Compiles linear pipelines executing in strict serial order.
2. **`ParallelExecutionStrategy`**: Automatically identifies independent nodes and stages them for concurrent execution.
3. **`GraphExecutionStrategy`**: Evaluates directed acyclic graphs (DAGs) using topological sorting.
4. **`ConditionalExecutionStrategy`**: Dynamically evaluates edge predicate conditions at runtime based on the `DecisionState`.
5. **`AgentExecutionStrategy`**: Manages autonomous ReAct / multi-agent reasoning and reflection loops.

---

## 7. Multi-Module Hierarchy

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
