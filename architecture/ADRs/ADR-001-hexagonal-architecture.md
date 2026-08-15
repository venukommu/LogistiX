# ADR 001: Adoption of Hexagonal Architecture (Ports & Adapters)

## Status
Accepted

## Context
LogistiX is an extensible AI platform designed for enterprise logistics and transportation. Logistics workflows involve multiple interchangeable technologies: different relational databases, vector stores, routing solvers (OR-Tools, OSRM, GraphHopper), and AI providers (OpenAI, Anthropic, local fine-tuned LLMs via Ollama/vLLM). Coupling core domain logic with framework code or database schemas would degrade maintainability, testing, and extensibility.

## Decision
We enforce a strict Hexagonal Architecture across all modules:
- `logistix-core`, `logistix-common`, and `logistix-decision-engine` remain 100% pure Java 21 with zero Spring or ORM annotations.
- Inbound interaction happens exclusively through Use Case ports (`org.logistix.core.port.inbound.*`).
- Outbound interaction (persistence, spatial calculations, telemetry) happens through Outbound ports (`org.logistix.core.port.outbound.*`).
- Infrastructure and frameworks live strictly in adapter modules (`logistix-starter`, `logistix-api`, `logistix-rag`, `logistix-ai`).

## Consequences
- **Positive**: Domain logic is fully decoupled and testable in pure unit tests without Spring context or mock web servers.
- **Positive**: AI providers, vector stores, and external TMS systems can be replaced or mocked seamlessly.
- **Negative**: Requires explicit mapping between API DTOs, domain models, and persistence entities.
