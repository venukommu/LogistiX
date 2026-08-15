# The LogistiX Framework Constitution

This document enshrines the foundational architectural tenets, design philosophies, and engineering principles governing the LogistiX Decision Intelligence Framework. Every future contribution, pull request, and architectural RFC must uphold these ten articles.

---

## Article I: Framework First
> *LogistiX is a generic framework, not an end-user application.*

LogistiX provides the abstractions, lifecycles, and execution topologies for AI-driven operational decision making. It must never contain business-specific domain silos, proprietary schemas, or bespoke CRUD handlers. Domain-specific applications (dispatching, dynamic pricing, carrier selection) are built *upon* LogistiX, never *inside* its core engine.

---

## Article II: Explainability Before Intelligence
> *A decision without an explanation is an operational liability.*

Every decision rendered by LogistiX must produce an immutable, audit-ready [`Explanation`](file:///Users/venukommu/Documents/LogistiX/Git/LogistiX/backend/logistix-domain/src/main/java/org/logistix/domain/explanation/Explanation.java). Black-box recommendations that cannot explain feature contributions, constraint boundaries, and rule outcomes are prohibited from entering core decision pathways.

---

## Article III: Rules Before AI
> *Deterministic guardrails always supersede probabilistic predictions.*

Hard physical, legal, and safety constraints (e.g., Hours of Service, Gross Vehicle Weight, Hazmat licensing) must be evaluated and enforced prior to or in conjunction with AI reasoning. Probabilistic models suggest; deterministic rules validate and protect.

---

## Article IV: AI Is Optional
> *The framework must function flawlessly with zero AI dependencies.*

A complete decision workflow can be composed purely of rule engines, scoring models, and constraint filters. AI models (LLMs, neural scorers, embeddings) are optional augmentations. The core framework must never require an active LLM provider or cloud connection to operate.

---

## Article V: Extensibility Over Specialization
> *Favor open SPIs and extension hooks over closed specialized features.*

Custom algorithms, third-party optimizers, proprietary heuristics, and novel telemetry providers must integrate seamlessly via Outbound Ports (`AIProvider`, `KnowledgeProvider`, `RuleProvider`, `ConstraintProvider`) and Lifecycle Plugins (`DecisionPlugin`, `DecisionHook`).

---

## Article VI: Execution Strategy Agnostic
> *Decouple WHAT executes from HOW it executes.*

A [`DecisionModel`](file:///Users/venukommu/Documents/LogistiX/Git/LogistiX/backend/logistix-model/src/main/java/org/logistix/model/model/DecisionModel.java) describes the topology of nodes and edges declaratively. The runtime execution engine compiles the model into an [`ExecutionPlan`](file:///Users/venukommu/Documents/LogistiX/Git/LogistiX/backend/logistix-model/src/main/java/org/logistix/model/plan/ExecutionPlan.java) using pluggable [`ExecutionStrategy`](file:///Users/venukommu/Documents/LogistiX/Git/LogistiX/backend/logistix-model/src/main/java/org/logistix/model/strategy/ExecutionStrategy.java) implementations (Sequential, Parallel, Graph DAG, Conditional Branching, ReAct Agent Loops) without modifying business logic.

---

## Article VII: Convention Over Configuration
> *Sensible defaults must enable instantaneous startup; deep customizability must remain accessible.*

Developers using LogistiX in Spring Boot or standalone Java should be able to execute decisions with zero boilerplate. Automatic classpath discovery, smart dependency defaults, and sensible timeouts must work out of the box.

---

## Article VIII: Developer Experience Matters
> *The public API is a product.*

The public API facade ([`LogistiX`](file:///Users/venukommu/Documents/LogistiX/Git/LogistiX/backend/logistix-dsl/src/main/java/org/logistix/dsl/LogistiX.java)) and its fluent DSLs must be intuitive, type-safe, and self-documenting. If an API requires consulting documentation to invoke a basic execution, it must be refined.

---

## Article IX: Backward Compatibility
> *Respect developer trust and production stability.*

Public APIs marked as **STABLE** in [`API_STABILITY.md`](file:///Users/venukommu/Documents/LogistiX/Git/LogistiX/docs/API_STABILITY.md) shall not undergo breaking changes. Deprecations must be signaled at least two minor releases in advance with complete migration guides.

---

## Article X: Simplicity Over Cleverness
> *Favor pure records, immutability, and explicit composition over complex inheritance and hidden magic.*

Use standard Java 21 features (Records, Sealed Interfaces, Pattern Matching). Avoid deep class inheritance hierarchies, magical reflection tricks, and ambiguous naming patterns (`Manager`, `Helper`, `Processor`).
