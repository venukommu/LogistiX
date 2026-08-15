# LogistiX

> **Open Source Framework for AI-Powered Operational Decision Making**
> *Explainable, Multi-Criteria Decision Intelligence with Fluent DSL & Spring Boot Integration*

[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.4.x-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Spring AI](https://img.shields.io/badge/Spring_AI-1.0.0-blueviolet.svg)](https://spring.io/projects/spring-ai)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17_pgvector-blue.svg)](https://github.com/pgvector/pgvector)

---

## 🚀 Mission & Vision

**LogistiX** is an extensible open-source framework for building **AI-powered operational decision systems**.

Inspired by the developer ergonomics of **Spring Boot**, the pipeline flexibility of **LangGraph**, the resiliency of **Temporal**, and the declarative fluency of **Apache Camel**, LogistiX empowers engineers to assemble multi-stage decision pipelines that combine:
- **Deterministic Hard Constraints & Feasibility Guardrails**
- **Business Policy Rules with Explicit Precedence**
- **AI / LLM Semantic Reasoning & RAG Retrieval**
- **Multi-Criteria Normalized Scoring & Explainability**

---

## ⚡ Quick Start: Hello World in 4 Lines

With `logistix-dsl`, running a decision requires zero boilerplate:

```java
import org.logistix.dsl.LogistiX;
import org.logistix.domain.decision.DecisionResult;

public class App {
    public static void main(String[] args) {
        DecisionResult<String> result = LogistiX.<String>decision("driver-dispatch")
                .fact("shipmentId", "SHIP-9901")
                .fact("origin", "Chicago, IL")
                .fact("destination", "Detroit, MI")
                .fact("weightLbs", 18500)
                .execute();

        System.out.println("Recommended Option: " + result.recommendation().item());
        System.out.println("Normalized Score: " + result.score().value());
        System.out.println("Confidence: " + result.confidence());
        System.out.println("Explanation: " + result.explanation().summary());
    }
}
```

---

## 🏗️ Assembling Pipelines with Fluent DSL

```java
// 1. Build an immutable decision pipeline
DecisionPipeline pipeline = LogistiX.pipeline("carrier-recommendation")
    .name("Standard-Carrier-Selection")
    .version("1.0.0")
    .step(new CarrierAvailabilityConstraintStep())
    .step(new ServiceLevelAgreementRuleStep())
    .step(new RouteRiskAiStep())
    .step(new MultiCriteriaScoringStep())
    .step(new ExplainableRecommendationStep())
    .build();

// 2. Register pipeline in the runtime container
LogistiX.getContext().getDecisionRegistry().register(pipeline);

// 3. Execute decision
DecisionResult<Carrier> result = LogistiX.<Carrier>decision("carrier-recommendation")
    .fact("lane", "LAX -> JFK")
    .execute();
```

---

## 🏷️ Declarative Annotations

LogistiX provides clean annotations for declarative component declaration:

```java
@DecisionRule(id = "RULE-PREMIUM-SLA", name = "Tier-1 Priority Boost", priority = 10)
public class PremiumCarrierRule implements Rule<CarrierCandidate> {
    @Override
    public RuleOutcome evaluate(CarrierCandidate carrier, DecisionContext context) {
        if ("TIER_1".equals(carrier.tier())) {
            return RuleOutcome.passed("RULE-PREMIUM-SLA", "Tier-1 Priority Boost", "Qualified for priority", 0.15);
        }
        return RuleOutcome.passed("RULE-PREMIUM-SLA", "Tier-1 Priority Boost", "Standard evaluation");
    }
}
```

- **`@DecisionPipeline("decision-type")`**: Declares a pipeline bean.
- **`@DecisionRule(id = "...", priority = 1)`**: Declares a business rule with priority.
- **`@DecisionConstraint(id = "...", severity = HARD)`**: Declares an operational guardrail.
- **`@DecisionPlugin(id = "...")`**: Declares a pluggable framework extension.

---

## 🍃 Spring Boot Auto-Configuration

Include `logistix-spring-boot-starter` in your `pom.xml`:

```xml
<dependency>
    <groupId>org.logistix</groupId>
    <artifactId>logistix-spring-boot-starter</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

Spring Boot automatically scans and registers all `@DecisionPipeline`, `@DecisionRule`, `@DecisionConstraint`, and `@DecisionPlugin` beans into `LogistiXContext` on startup!

### Configuration Properties (`application.yml`)

```yaml
logistix:
  enabled: true
  default-timeout: 10s
  trace-level: DETAILED
  strict-constraints: true
  fail-fast-on-rule-error: false
  auto-discovery: true
```

---

## 📐 Architecture & Decision Flow

```mermaid
flowchart TD
    subgraph Client ["1. Invocation"]
        REQ["<b>DecisionRequest&lt;T&gt;</b> or <b>LogistiX.decision()</b>"]
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

## 📂 Repository Structure

```
LogistiX/
├── backend/
│   ├── pom.xml                        # Master Parent POM (Java 21, Dependency Management)
│   ├── logistix-common/               # Shared Value Objects, Exceptions, Utilities (Pure Java 21)
│   ├── logistix-domain/               # Pure Domain Layer: DecisionContext, Facts, Rules, Ports
│   ├── logistix-engine/               # Framework Execution Runtime: Pipelines, Steps, Traces, Plugins
│   ├── logistix-dsl/                  # Public API Facade, Fluent DSLs, Annotations, Builders, CLI
│   ├── logistix-decision-engine/      # Composite Pipeline Orchestrators & Strategy Registry
│   ├── logistix-ai/                   # AI Provider Abstractions, Prompts, Tool Calling via Spring AI
│   ├── logistix-rag/                  # Knowledge Ingestion, Retrievers & pgvector Integration
│   ├── logistix-simulation/           # Synthetic Fleet, Demand, Weather & Traffic Simulators
│   ├── logistix-benchmark/            # Model, Rule Engine, and Decision Pipeline Evaluators
│   ├── logistix-spring-boot-starter/  # Spring Boot AutoConfiguration, Scanning & Properties Binding
│   ├── logistix-starter/              # Core Starter Wiring
│   └── logistix-api/                  # REST Gateway, OpenAPI 3, and Global Exception Handling
├── examples/                          # Self-contained executable code samples & tutorials
├── frontend/                          # Dispatcher UI & Map Visualizers (Reserved)
├── datasets/                          # Benchmark Logistics Datasets & Telemetry Schemas
├── training/                          # Fine-tuning recipes & offline ML pipelines
├── docs/                              # Project Documentation & Architecture Guides
├── architecture/                      # Architectural specs, C4 diagrams, and ADRs
│   └── ADRs/                          # Architecture Decision Records
└── docker/
    ├── docker-compose.yml             # PostgreSQL 17 + pgvector service
    └── postgres/
        └── 01-init-pgvector.sql       # Vector extension initialization script
```

---

## 🛠️ Build & Verification

```bash
cd backend
mvn clean test-compile
```

---

## 📄 License

LogistiX is open-source software licensed under the [Apache License, Version 2.0](LICENSE).
