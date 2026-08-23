# LogistiX Governed AI Action Architecture

## 1. Core Architectural Thesis

> **"AI proposes. LogistiX governs. LogistiX authorizes. Only the exact authorized action executes. MCP provides connectivity. Enterprise systems remain protected behind the decision boundary."**

> **"Only trusted LogistiX issuance components can create executable authorization artifacts."**

> **"Trust configuration is established at startup, validated once, then immutable during runtime."**

In enterprise logistics operations (TMS, Fleet Management, ERP), an AI model, LLM agent, or heuristic algorithm must **never** be granted autonomous write access to enterprise tools.

LogistiX enforces a strict, technology-neutral authorization boundary where intelligence producers can only generate unverified `ActionProposal` instances. Every proposal must pass deterministic policy, risk, and constraint evaluations before an immutable `AuthorizedAction` is minted by a trusted `ActionAuthorizationIssuer`.

---

## 2. Governed Action Execution Flow

```
                     AI / APPLICATION
                           │
                           ▼
                    ActionProposal
                           │
                           ▼
                  LOGISTIX GOVERNANCE
                           │
                ┌──────────┴──────────┐
                │                     │
                ▼                     ▼
         Approval Required         Approved
                │                     │
                ▼                     ▼
       Trusted Approval Issuer   Trusted Authorization Issuer
        (ActionApprovalIssuer)  (ActionAuthorizationIssuer)
                │                     │
                ▼                     ▼
       ActionApprovalGrant       AuthorizedAction
        (ApprovalProvenance)     (AuthorizationProvenance +
                │                 SHA-256 Fingerprint)
                │                     │
                └──────────┬──────────┘
                           ▼
                     ActionExecutor
                    (McpActionExecutor)
                           │
                           ▼
                       MCP Adapter
                           │
                           ▼
                     Tool Registry
                      (Frozen Map)
                           │
                           ▼
                     Enterprise Tool
```

---

## 3. Trusted Issuer & Approver Configuration

```
             application.yml
                    │
                    ▼
            LogistiX Properties
                    │
                    ▼
         Security Configuration
                    │
            ┌───────┴────────┐
            ▼                ▼
      Authority Registry  Approver Registry
            │                │
            └───────┬────────┘
                    ▼
                 VALIDATE
                    │
                    ▼
                  FREEZE
                    │
                    ▼
             RUNTIME READ ONLY
                    │
          ┌─────────┴─────────┐
          ▼                   ▼
    Authorization Issuer   Approval Issuer
```

1. **Startup Lifecycle**:
   - Security properties are read from `application.yml` via `LogistiXProperties`.
   - `AuthorizationAuthorityRegistry` and `TrustedApproverRegistry` are populated during application initialization.
   - Registries are validated (rejecting blank or duplicate IDs) and frozen via `freeze()`.
   - Any runtime attempt to mutate registries throws `IllegalStateException`.
2. **Closed Issuance Boundary**:
   - `AuthorizedAction` is an immutable `final class` with package-private construction restricted to trusted `ActionAuthorizationIssuer` implementations.
   - Public convenience factories (`AuthorizedAction.of`, `AuthorizedAction.createForTesting`) are eliminated from production code.
3. **Trusted Authorization Authority Verification**:
   - `AuthorizationProvenance` records issuer authority metadata.
   - `McpActionExecutor` validates provenance against the in-process `AuthorizationAuthorityRegistry`.
4. **Tamper-Evident Integrity via Canonical SHA-256 Fingerprint**:
   - Every `AuthorizedAction` carries an immutable `authorizationFingerprint` computed deterministically over:
     `actionType`, `targetResource`, recursively canonicalized `parameters`, `policyApplied`, `issuerId`, `correlationId`, `idempotencyKey`, and `expiresAt`.
   - Modifying any parameter (including nested collections) or swapping a target resource after authorization triggers a fingerprint mismatch and halts execution.
5. **Exact-Boundary Expiration**:
   - Authorizations possess an `expiresAt` window (default 5 minutes). At `now >= expiresAt`, the authorization is strictly treated as expired.
6. **Atomic Idempotency Reservation**:
   - Concurrent submissions with the same idempotency key are atomically reserved, guaranteeing exactly one execution.
7. **Immutable Frozen Tool Registry**:
   - `ToolRegistry` enforces a strict lifecycle (`configure` $\to$ `freeze`), rejecting tool registration or modification during execution.

---

## 4. Human Approval Issuance & Revalidation Lifecycle

When an action is classified as `APPROVAL_REQUIRED`:

```
ActionProposal (High Risk)
     ↓
Governance Evaluation → APPROVAL_REQUIRED (0 MCP Calls)
     ↓
Operational Supervisor Grants Approval
     ↓
Trusted Approval Issuer (ActionApprovalIssuer)
  ├── Validate Approver against Frozen TrustedApproverRegistry
  ├── Generate ApprovalProvenance
  └── Mint Single-Use ActionApprovalGrant
     ↓
Governance Revalidation (DefaultActionGovernanceEngine.revalidateAndAuthorize)
  ├── Verify Approval Provenance is Valid
  ├── Verify Grant Has Not Been Consumed (Atomic Single-Use Check)
  ├── Verify Grant Action ID == Proposal Action ID
  ├── Verify Grant Target Resource == Proposal Target Resource
  ├── Verify Grant Proposal Fingerprint == Current Proposal Fingerprint
  └── Re-verify HARD Constraints
     ↓
Grant Marked Consumed
     ↓
Trusted Authorization Issuer Mints Fresh AuthorizedAction
     ↓
ActionExecutor Dispatches to MCP
```

---

## 5. Reference Implementation vs Production Architecture

| Capability | LogistiX Reference Implementation | Future Production Deployment |
| :--- | :--- | :--- |
| **Trust Configuration** | Validated & Frozen in-process registries | Central Configuration / Vault / Spring Cloud Config |
| **Authorization Issuance** | In-Process `ActionAuthorizationIssuer` | Central Authorization Service with HSM/KMS |
| **Approval Issuance** | In-Process `DefaultActionApprovalIssuer` | Enterprise IAM / SSO / BPMN Gateway (ServiceNow/Okta) |
| **Approver Registry** | In-Memory Frozen `TrustedApproverRegistry` | Enterprise RBAC / ABAC Directory (LDAP/SCIM) |
| **Audit Storage** | In-Memory `InMemoryActionAuditStore` | Append-only WORM event store (PostgreSQL/Kafka) |
| **Idempotency** | In-Memory Atomic Reservation | Distributed Redis / Hazelcast idempotency lock |
| **Tool Execution** | Deterministic `MockMcpToolServer` | Production MCP Transport (stdio / SSE) with mTLS |
| **Integrity Check** | Canonical SHA-256 Fingerprint | Canonical SHA-256 + Signed Payload Envelope (Ed25519) |
