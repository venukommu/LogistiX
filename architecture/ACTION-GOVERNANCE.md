# LogistiX Governed AI Action Architecture

## 1. Core Architectural Thesis

> **"AI proposes. LogistiX governs. LogistiX authorizes. Only the exact authorized action executes. MCP provides connectivity. Enterprise systems remain protected behind the decision boundary."**

> **"Only trusted LogistiX issuance components can create executable authorization artifacts."**

> **"Trust configuration is established at startup, validated once, then immutable during runtime."**

> **"MCP is an optional infrastructure adapter. The core Spring Boot starter does not require MCP."**

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
                      (logistix-mcp)
                           │
                           ▼
                     Tool Registry
                      (Frozen Map)
                           │
                           ▼
                     Enterprise Tool
```

---

## 3. Security Configuration Lifecycle & Starter Decoupling

```
             application.yml
                    │
                    ▼
            LogistiX Properties
          (LogistiXProperties)
                    │
                    ▼
         Security Configuration
                    │
            ┌───────┴────────┐
            ▼                ▼
     Authority Registry   Approver Registry
   (AuthorizationAuth)   (TrustedApprover)
            │                │
            └───────┬────────┘
                    ▼
                 VALIDATE
       (fail fast on mismatch)
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

1. **Modular Decoupling**:
   - `logistix-spring-boot-starter` provides core decision intelligence and governance without any mandatory dependency on MCP.
   - `logistix-mcp` is an optional infrastructure adapter with its own dedicated auto-configuration (`LogistiXMcpAutoConfiguration`).
2. **Canonical `authorityId` Identity**:
   - `authorityId` is the single canonical authorization authority identity.
   - Configured via `logistix.security.authorization.authority-id`.
   - The startup lifecycle validates that the configured `authorityId` is registered in `authorities` and fails fast if missing or conflicting.
3. **Explicit, Safe Approver Configuration (Option A)**:
   - When no approvers are explicitly declared in `logistix.security.approvers`, the system provisions an empty, frozen `TrustedApproverRegistry` (rejecting all unconfigured human approvals).
   - No implicit wildcard or default supervisor identities are silently created.
4. **Closed Issuance Boundary**:
   - `AuthorizedAction` is an immutable `final class` with package-private construction restricted to trusted `ActionAuthorizationIssuer` implementations.
5. **Tamper-Evident Integrity via Canonical SHA-256 Fingerprint**:
   - Every `AuthorizedAction` carries an immutable `authorizationFingerprint` computed deterministically over:
     `actionType`, `targetResource`, recursively canonicalized `parameters`, `policyApplied`, `issuerId`, `correlationId`, `idempotencyKey`, and `expiresAt`.
6. **Exact-Boundary Expiration**:
   - Authorizations possess an `expiresAt` window (default 5 minutes). At `now >= expiresAt`, the authorization is strictly treated as expired.
7. **Atomic Idempotency Reservation**:
   - Concurrent submissions with the same idempotency key are atomically reserved, guaranteeing exactly one execution.
8. **Immutable Frozen Tool Registry**:
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

## 5. Configuration Examples

### Minimal Core Starter Configuration (MCP Decoupled)
```yaml
logistix:
  security:
    enabled: true
    authorization:
      authority-id: LogistiX-Governance-Authority
      authorities:
        - LogistiX-Governance-Authority
        - LogistiX-Authority-Primary
    approvers:
      - id: SUPERVISOR-001
        allowed-action-types:
          - CHANGE_DELIVERY_APPOINTMENT
          - ASSIGN_DRIVER
        enabled: true
```

### With Optional MCP Adapter Enabled (`logistix-mcp`)
```yaml
logistix:
  security:
    enabled: true
    authorization:
      authority-id: LogistiX-Governance-Authority
      authorities:
        - LogistiX-Governance-Authority
  mcp:
    enabled: true
    execution-timeout: 10s
```

---

## 6. Reference Trust Model

The LogistiX reference implementation uses in-process, technology-neutral trust components:

```
Application Configuration (application.yml)
        ↓
Validate Configuration (Fail fast on duplicate/unregistered/conflicting IDs)
        ↓
Build Trusted Registries (AuthorizationAuthorityRegistry, TrustedApproverRegistry)
        ↓
Freeze Registries (State becomes strictly immutable AtomicBoolean frozen=true)
        ↓
Runtime Read-Only Access
```

- **`AuthorizationAuthorityRegistry`**: In-process reference registry of trusted authorization authority identities. Provides startup registration and frozen runtime read-only validation. It is not an external identity provider or KMS.
- **`TrustedApproverRegistry`**: In-process reference registry of authorized operational approvers and their permitted action types.
- **`ActionAuthorizationIssuer`**: Trusted domain issuance component responsible for computing canonical fingerprints, setting provenance, and minting `AuthorizedAction` instances.
- **`ActionApprovalIssuer`**: Trusted domain issuance component responsible for verifying human supervisor credentials and minting single-use `ActionApprovalGrant` instances.
- **`McpActionExecutor`**: Outbound infrastructure adapter that executes only verified, non-expired, tamper-checked `AuthorizedAction` instances against registered enterprise tools.

---

## 7. Reference Implementation vs Production Architecture

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
