# Sprint 10 Baseline Audit: Governed AI Actions & MCP Adapter

**Date**: 2026-08-23  
**Status**: BASELINE ESTABLISHED  
**Baseline Test Status**: 54/54 tests passing across all 13 modules (100% BUILD SUCCESS)

---

## 1. Current Architecture & Decision Pipeline Topology

LogistiX currently implements a production-hardened, auditable Decision Intelligence pipeline:

```
┌─────────────────────────┐
│ 1. Decision Context     │  Shipment order, operational environment, route coordinates
└───────────┬─────────────┘
            ▼
┌─────────────────────────┐
│ 2. Hard Constraints     │  Hours-of-Service (HOS), Payload capacity, Mandatory certifications
└───────────┬─────────────┘
            ▼
┌─────────────────────────┐
│ 3. Business Rules       │  Carrier tier bonuses, loyalty incentives, soft preferences
└───────────┬─────────────┘
            ▼
┌─────────────────────────┐
│ 4. Deterministic Score  │  Multi-criteria weighted composite scoring & Top-N ranking
└───────────┬─────────────┘
            ▼
┌─────────────────────────┐
│ 5. Knowledge Retrieval  │  Enterprise operating standards (DOC-WINTER-001) retrieved via KnowledgeProvider SPI
└───────────┬─────────────┘
            ▼
┌─────────────────────────┐
│ 6. Single Batched AI    │  Qualitative risk analysis across top-N feasible candidates (Prompt V2)
└───────────┬─────────────┘
            ▼
┌─────────────────────────┐
│ 7. Citation Validation  │  Filter & de-duplicate evidence citations against retrieved documents
└───────────┬─────────────┘
            ▼
┌─────────────────────────┐
│ 8. Deterministic Policy │  Evaluates AI advisory signals; selects candidate with full explainability
└───────────┬─────────────┘
            ▼
┌─────────────────────────┐
│ 9. Final Decision       │  Auditable DecisionResult with segregated factors, evidence & telemetry
└─────────────────────────┘
```

---

## 2. Existing Outbound SPIs & Module Graph

### Existing Outbound Ports (`logistix-domain/ports`)
- `AIProvider`: Infer contextual risk advice and generate explanations.
- `KnowledgeProvider`: Retrieve relevant grounding reference documents.
- `RuleProvider`: Supply declarative business rules.
- `ConstraintProvider`: Supply deterministic hard constraints.
- `ScoringProvider`: Supply mathematical objective scoring algorithms.

### Clean Architecture Isolation
- `logistix-domain` contains **zero** external framework dependencies (pure Java 21).
- All infrastructure/external integrations (`Spring AI`, `RAG`) live in peripheral adapter modules (`logistix-ai`, `logistix-rag`).

---

## 3. Sprint 10 Objective & Target Architecture

### Core Principle
> *"MCP provides connectivity. LogistiX provides authorization."*  
> *"AI proposes. LogistiX governs. Only authorized actions execute."*

### Architecture Flow
```
                         AI / Decision Producer
                                   │
                                   ▼
                            ActionProposal
                                   │
                                   ▼
                      ┌─────────────────────────┐
                      │    LOGISTIX GOVERNANCE  │
                      │                         │
                      │ Business Rules          │
                      │ Constraints             │
                      │ Permissions             │
                      │ Risk & Confidence       │
                      │ Human Approval Checks   │
                      │ Audit & Idempotency     │
                      └────────────┬────────────┘
                                   │
                        ┌──────────┼──────────┐
                        │          │          │
                        ▼          ▼          ▼
                      REJECT    APPROVAL    APPROVE
                                   │
                                   │
                                   ▼
                            AuthorizedAction
                                   │
                                   ▼
                              ActionPort
                           (ActionExecutor)
                                   │
                                   ▼
                              MCP Adapter
                          (McpActionExecutor)
                                   │
                                   ▼
                              MCP Protocol
                                   │
                      ┌────────────┼────────────┐
                      ▼            ▼            ▼
                    Tool A       Tool B       Tool C
```

---

## 4. Proposed Module & Contract Plan

1. **`logistix-domain` (Technology-Neutral Action Contracts)**:
   - `org.logistix.domain.action`: `ActionProposal`, `ActionDecision`, `ActionStatus` (`APPROVED`, `REJECTED`, `APPROVAL_REQUIRED`), `AuthorizedAction`, `ActionResult`, `ActionType`, `ActionAuditEntry`, `ActionTelemetry`.
   - `org.logistix.domain.ports`: `ActionExecutor` (outbound execution SPI), `ActionPolicyProvider`.
   - **Zero MCP dependencies in domain**.

2. **`logistix-engine` (Deterministic Action Governance)**:
   - `org.logistix.engine.action`: `ActionGovernanceEngine`, `ActionPolicy`, `DefaultActionGovernanceEngine`, `ActionAuditLog`.

3. **`logistix-mcp` (New Module: MCP Adapter & Mock Tools)**:
   - `org.logistix.mcp`: `McpActionExecutor` (implements `ActionExecutor`), `ToolRegistry`, `McpToolDefinition`, `MockMcpToolServer` (local mock tools: `changeDeliveryAppointment`, `assignDriver`, `updateShipmentStatus`).

4. **`logistix-examples` (Governed Action Demonstration & Test Suite)**:
   - Three core scenarios:
     - **Scenario A**: Low-risk valid action -> `APPROVED` -> MCP invoked (1 call).
     - **Scenario B**: Hard-constraint/permission violating action -> `REJECTED` -> 0 MCP calls.
     - **Scenario C**: High-risk action -> `APPROVAL_REQUIRED` -> 0 MCP calls without approval.
