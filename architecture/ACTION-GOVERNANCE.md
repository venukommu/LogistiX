# LogistiX Governed AI Action Architecture

## 1. Core Architectural Thesis

> **"AI proposes. LogistiX governs. LogistiX authorizes. Only the exact authorized action executes. MCP provides connectivity. Enterprise systems remain protected behind the decision boundary."**

In enterprise logistics operations (TMS, Fleet Management, ERP), an AI model, LLM agent, or heuristic algorithm must **never** be granted autonomous write access to enterprise tools.

LogistiX enforces a strict, technology-neutral authorization boundary where intelligence producers can only generate unverified `ActionProposal` instances. Every proposal must pass deterministic policy, risk, and constraint evaluations before an immutable `AuthorizedAction` is minted.

---

## 2. Governed Action Execution Flow

```
                       AI / Decision Producer
                                 │
                                 ▼
                           ActionProposal
                                 │
                                 ▼
                     ┌───────────────────────┐
                     │  LOGISTIX GOVERNANCE  │
                     │                       │
                     │  1. Idempotency Check │
                     │  2. Action Whitelist  │
                     │  3. HARD Constraints  │
                     │  4. Risk Threshold    │
                     │  5. Confidence Check  │
                     └───────────┬───────────┘
                                 │
                     ┌───────────┼───────────┐
                     ▼           ▼           ▼
                  REJECT      APPROVAL    APPROVE
                     │        REQUIRED       │
                     │           │           ▼
                     │           │    AuthorizedAction
                     │           │    (SHA-256 Token)
                     │           │           │
                     │           │           ▼
                     │           │     ActionExecutor
                     │           │    (McpActionExec)
                     │           │           │
                     │           │           ▼
                     │           │          MCP
                     │           │           │
                     │           │           ▼
                     │           │    Enterprise Tool
                     │           │   (TMS / Fleet / DB)
                     │           │
                     └── NO EXECUTION ───────┘
```

---

## 3. Fundamental Invariants

1. **Proposal != Authorization**:
   - `ActionProposal` is strictly an advisory request. Passing an `ActionProposal` to an `ActionExecutor` or `McpActionExecutor` is architecturally prohibited by strong Java type typing.
2. **Authorization != Execution**:
   - Creating an `AuthorizedAction` does not execute it. Execution happens only when explicitly dispatched to an `ActionExecutor`.
3. **MCP != Governance**:
   - MCP is purely an infrastructure connectivity transport. MCP has **zero** authority to approve actions. The core domain (`logistix-domain`) is 100% free of MCP dependencies.
4. **Exact Action Binding via SHA-256 Fingerprint**:
   - Every `AuthorizedAction` carries an immutable `authorizationFingerprint` computed deterministically over:
     `actionType`, `targetResource`, sorted `parameters`, `policyApplied`, `correlationId`, `idempotencyKey`, and `expiresAt`.
   - Modifying a parameter or swapping a target resource after authorization immediately triggers a fingerprint mismatch and halts execution.
5. **Clock-Based Expiration Window**:
   - Authorizations possess a configurable validity duration (`expiresAt`). Expired authorizations cannot execute.
6. **Strict Schema & Controlled Tool Registry**:
   - `ToolRegistry` contains an explicit whitelist of registered tools. AI cannot invent arbitrary tool names or supply malicious MCP server endpoints.

---

## 4. Human Approval Revalidation Lifecycle

When an action is classified as `APPROVAL_REQUIRED` (e.g. high-risk schedule changes or low confidence):

```
ActionProposal (High Risk)
     ↓
Governance Evaluation → APPROVAL_REQUIRED (0 MCP Calls)
     ↓
Operational Supervisor Grants Approval (ActionApprovalGrant)
     ↓
Governance Revalidation (revalidateAndAuthorize)
  ├── Verify Grant Action ID == Proposal Action ID
  ├── Verify Grant Target Resource == Proposal Target Resource
  └── Re-verify HARD Constraints
     ↓
Fresh AuthorizedAction Issued
     ↓
ActionExecutor Dispatches to MCP
```

---

## 5. Reference Implementation Limitations & Production Roadmap

| Capability | Sprint 10.1 Reference Implementation | Production Enterprise Target |
| :--- | :--- | :--- |
| **Audit Storage** | In-Memory `InMemoryActionAuditStore` | Distributed immutable append-only event log (e.g., Kafka / PostgreSQL WORM) |
| **Idempotency** | In-Memory `ConcurrentHashMap` | Distributed Redis / Hazelcast idempotency lock |
| **Tool Execution** | Deterministic `MockMcpToolServer` | Production MCP Transport (stdio / SSE) with mTLS |
| **Authorization Token** | Canonical SHA-256 Fingerprint + Token | Asymmetric Cryptographic Signing (Ed25519 / HMAC) |
| **Approval Workflow** | In-Memory `ActionApprovalGrant` | Enterprise BPMN / ServiceNow / Slack approval gateway |
