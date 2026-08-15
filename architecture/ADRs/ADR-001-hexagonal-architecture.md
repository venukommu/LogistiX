# ADR 001: Adoption of Hexagonal Architecture & Pure Domain Decision Framework

## Status
Accepted (Updated in Sprint 2)

## Context
LogistiX is designed as a reusable, open-source framework for AI-powered operational decision making across logistics, transportation, and supply chain domains. Operational decisions (dispatching, routing, pricing, carrier selection, dock scheduling, fraud detection) involve rapidly evolving technologies: multiple LLM vendors, vector databases, optimization solvers, and enterprise TMS/WMS backends.

Coupling the decision domain with frameworks (Spring, Hibernate, REST, specific AI SDKs) would make the framework rigid, prevent domain-level reuse across non-Spring runtimes, and severely complicate unit testing.

## Decision
We enforce strict Hexagonal Architecture across all modules:
1. **`logistix-domain` is 100% Pure Java 21**:
   - Zero Spring framework annotations or dependencies.
   - Zero REST / web dependencies.
   - Zero AI provider SDKs.
2. **`DecisionContext` is the Central Domain Object**:
   - Holds an extensible, immutable `FactBag` for arbitrary domain facts, decoupling the engine from specific verticals.
3. **Pluggable Outbound SPIs**:
   - All external capabilities (`AIProvider`, `KnowledgeProvider`, `RuleProvider`, `ConstraintProvider`, `ScoringProvider`, `DomainEventPublisher`) are defined as pure Java interfaces in `org.logistix.domain.ports`.
4. **Separation of Infrastructure Adapters**:
   - Infrastructure, web endpoints, database persistence, and Spring Boot auto-configuration live strictly in adapter modules (`logistix-starter`, `logistix-api`, `logistix-rag`, `logistix-ai`).

## Consequences
- **Positive**: Complete domain portability and pure unit testability without Spring context startup overhead.
- **Positive**: Extensibility to arbitrary operational decision types beyond dispatching.
- **Positive**: Pluggable AI engines (OpenAI, Anthropic, local vLLM, DeepSeek) without changing a single line of domain code.
- **Negative**: Requires clean mapping between API/external DTOs and domain records.
