# LogistiX Architecture Specification

## Overview & Mission
**LogistiX** is an open-source AI platform for logistics and transportation with explainable decision intelligence. It is designed to bridge domain operations (dispatching, routing, freight pricing, ETA prediction) with modern AI capabilities (LLM tool-calling, RAG, multi-agent coordination, and custom fine-tuned models).

---

## Architectural Principles

1. **Hexagonal / Clean Architecture (Ports & Adapters)**:
   - The domain core (`logistix-core`, `logistix-common`, `logistix-decision-engine`) is completely isolated from frameworks, databases, and network transports.
   - External dependencies (REST, PostgreSQL, pgvector, LLM APIs) interact strictly via Inbound and Outbound Ports (SPIs).
2. **Explainability by Design**:
   - Every AI recommendation or algorithmic decision produces a structured `DecisionExplanation` capturing feature importances, trade-offs, confidence scores, and rule outcomes.
3. **Constructor Injection Only**:
   - All components and Spring beans rely strictly on constructor injection for immutability, testability, and explicit dependency graphs.
4. **No Circular Dependencies**:
   - Strict hierarchical acyclic module dependency graph (DAG) enforced at build time.

---

## High-Level System Architecture

```mermaid
graph TB
    subgraph Client Layer
        UI[Web UI / Dispatch Dashboard]
        TMS[External TMS / WMS Systems]
    end

    subgraph "logistix-api (Inbound Adapter)"
        REST[REST Controllers & OpenAPI]
        ExceptionHandler[Global Exception Handler]
        Actuator[Actuator & Metrics]
    end

    subgraph "logistix-starter (Auto-Configuration & Wiring)"
        Wiring[Spring AutoConfiguration & Properties]
    end

    subgraph "logistix-core (Hexagonal Core - Pure Java 21)"
        PortsIn[Inbound Ports<br/><i>DispatchUseCase, RouteOptimizationUseCase</i>]
        Domain[Domain Models & Events<br/><i>Shipment, Driver, Route, Vehicle</i>]
        PortsOut[Outbound Ports<br/><i>ShipmentRepositoryPort, DriverRepositoryPort</i>]
    end

    subgraph "logistix-decision-engine"
        Decision[Decision Engine & Rule Engine]
        Explain[Explainability Models & Feature Importance]
    end

    subgraph "logistix-ai"
        ModelProviders[Model Providers & Prompt Templates]
        Tools[Tool Calling Abstractions]
    end

    subgraph "logistix-rag"
        Retriever[Knowledge Retriever]
        Embeddings[Embedding Abstractions]
        VectorStore[Vector Store Ports]
    end

    subgraph Infrastructure
        PG[(PostgreSQL + pgvector)]
        LLM[External LLM Providers]
    end

    UI --> REST
    TMS --> REST
    REST --> PortsIn
    PortsIn --> Domain
    PortsIn --> Decision
    Decision --> Explain
    Decision --> ModelProviders
    ModelProviders --> LLM
    Retriever --> Embeddings
    Retriever --> VectorStore
    VectorStore --> PG
    PortsOut --> PG
```

---

## Module Boundaries

| Module | Framework Coupling | Description |
| :--- | :--- | :--- |
| `logistix-common` | **None** (Pure Java 21) | Base value objects, exceptions, pagination, standard utilities |
| `logistix-core` | **None** (Pure Java 21) | Core domain entities, immutable records, domain events, inbound/outbound ports |
| `logistix-decision-engine` | **None** (Pure Java 21) | Decision contracts, rule engines, explainability records, recommendation scorers |
| `logistix-ai` | Spring AI Core | Model provider interfaces, prompt abstractions, function/tool calling contracts |
| `logistix-rag` | Spring AI, pgvector | Knowledge document chunking, embeddings, similarity query models, vector store SPI |
| `logistix-starter` | Spring Boot Starter | Autoconfiguration, `@ConfigurationProperties` binding, IoC assembly |
| `logistix-api` | Spring Web, Springdoc | REST API endpoints, OpenAPI documentation, Actuator telemetry, RFC 7807 handler |
