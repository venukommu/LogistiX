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
                     │  1. Atomic Idemp Check│
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
                     │           │ (Immutable Class +
                     │           │  SHA-256 Fingerprint +
                     │           │  Provenance Token)
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

## 3. Fundamental Invariants & Provenance Integrity

1. **Proposal != Authorization**:
   - `ActionProposal` is strictly an advisory request. Passing an `ActionProposal` to an `ActionExecutor` is impossible via the strongly typed API.
2. **Controlled Issuance & Authorization Provenance**:
   - `AuthorizedAction` is an immutable `final class` (eliminating the public canonical record constructor).
   - Only `LogistiX Governance` can mint authentic authorizations with valid `AuthorizationProvenance`.
3. **Tamper-Evident Integrity via Canonical SHA-256 Fingerprint**:
   - Every `AuthorizedAction` carries an immutable `authorizationFingerprint` computed deterministically over:
     `actionType`, `targetResource`, recursively canonicalized `parameters`, `policyApplied`, `issuerId`, `correlationId`, `idempotencyKey`, and `expiresAt`.
   - Modifying any parameter (including nested collections) or swapping a target resource after authorization triggers a fingerprint mismatch and halts execution.
4. **Deterministic Parameter Canonicalization**:
   - Canonicalized via `ParameterCanonicalizer`:
     - Map keys sorted lexicographically (`M:{k1=v1,k2=v2}`).
     - Set elements deterministically sorted (`SET:[e1,e2]`).
     - List element order preserved (`L:[e1,e2]`).
     - Explicit typed prefixes (`S:`, `N:`, `B:`, `E:`, `null`) prevent delimiter collision.
5. **Exact-Boundary Expiration**:
   - Authorizations possess an `expiresAt` window (default 5 minutes). At `now >= expiresAt`, the authorization is strictly treated as expired.
6. **Atomic Idempotency Reservation**:
   - Concurrent submissions with the same idempotency key are atomically reserved, guaranteeing exactly one execution.
7. **Immutable Frozen Tool Registry**:
   - `ToolRegistry` enforces a strict lifecycle (`configure` $\to$ `freeze`), rejecting tool registration or modification during execution.

---

## 4. Human Approval Revalidation Lifecycle

When an action is classified as `APPROVAL_REQUIRED`:

```
ActionProposal (High Risk)
     ↓
Governance Evaluation → APPROVAL_REQUIRED (0 MCP Calls)
     ↓
Operational Supervisor Grants Approval (ActionApprovalGrant with Proposal Fingerprint)
     ↓
Governance Revalidation (revalidateAndAuthorize)
  ├── Verify Grant Has Not Been Consumed (Atomic Single-Use Check)
  ├── Verify Grant Action ID == Proposal Action ID
  ├── Verify Grant Target Resource == Proposal Target Resource
  ├── Verify Grant Proposal Fingerprint == Current Proposal Fingerprint
  └── Re-verify HARD Constraints
     ↓
Grant Marked Consumed
     ↓
Fresh AuthorizedAction Issued
     ↓
ActionExecutor Dispatches to MCP
```

---

## 5. Reference Implementation vs Production Architecture

| Capability | LogistiX Reference Implementation | Future Production Deployment |
| :--- | :--- | :--- |
| **Audit Storage** | In-Memory `InMemoryActionAuditStore` | Append-only WORM event store (PostgreSQL/Kafka) |
| **Idempotency** | In-Memory Atomic Reservation | Distributed Redis / Hazelcast idempotency lock |
| **Tool Execution** | Deterministic `MockMcpToolServer` | Production MCP Transport (stdio / SSE) with mTLS |
| **Issuer Authentication**| Reference `AuthorizationProvenance` + Token | Asymmetric Cryptographic Signing (Ed25519 / HMAC) |
| **Integrity Check** | Canonical SHA-256 Fingerprint | Canonical SHA-256 + Signed Payload Envelope |
| **Approval Workflow** | In-Memory `ActionApprovalGrant` | Enterprise BPMN / ServiceNow approval gateway |
