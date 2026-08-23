# Sprint 10.1 Baseline Audit: Action Authorization & Execution Boundary Hardening

**Date**: August 23, 2026  
**Status**: AUDIT COMPLETE (Phase 0)  
**Baseline Test Count**: 74 Tests Passing across 14 Modules (0 Failures, 0 Errors, 0 Skipped)  

---

## 1. Current State Assessment

In Sprint 10, LogistiX introduced the Governed AI Action boundary:
`ActionProposal` $\to$ `ActionGovernanceEngine` $\to$ `ActionDecision` $\to$ `AuthorizedAction` $\to$ `ActionExecutor` (`McpActionExecutor`) $\to$ `MockMcpToolServer`.

### Baseline Modules:
- `logistix-domain`: Contains `ActionProposal`, `AuthorizedAction`, `ActionDecision`, `ActionStatus`, `ActionResult`, `ActionTelemetry`, `ActionAuditEntry`, and `ActionExecutor`.
- `logistix-engine`: Contains `ActionPolicy`, `ActionGovernanceEngine`, `DefaultActionGovernanceEngine`, `InMemoryActionAuditStore`.
- `logistix-mcp`: Contains `McpToolDefinition`, `ToolRegistry`, `MockMcpToolServer`, `McpActionExecutor`, `LogistiXMcpProperties`.
- `logistix-examples`: Contains unit/integration tests (`ActionGovernanceTest`, `McpAdapterTest`, `GovernedActionSecurityTest`).

---

## 2. Attack Vectors & Hardening Opportunities Identified

1. **`AuthorizedAction` Construction Access**:
   - `AuthorizedAction` currently has a public constructor. Any external caller could invoke `new AuthorizedAction(...)` or `AuthorizedAction.of(...)`.
   - **Hardening Requirement**: Protect `AuthorizedAction` construction so that only the authorized governance boundary can issue valid instances. Introduce an authorization factory / sealed token mechanism.

2. **Action Fingerprint & Cryptographic Parameter Binding**:
   - Parameters and action components must have an immutable canonical SHA-256 fingerprint (`authorizationFingerprint`) derived from: `actionType`, `targetResource`, normalized `parameters`, `toolName`, `policyApplied`, `correlationId`, `idempotencyKey`, and `expiresAt`.
   - **Hardening Requirement**: If an action's parameters or target resource are tampered with or modified after authorization, `authorizationFingerprint` validation immediately detects the mismatch and rejects execution.

3. **Authorization Expiration & Clock-Based Validity Window**:
   - Currently, `AuthorizedAction` only contains `authorizedAt` without an `expiresAt` check.
   - **Hardening Requirement**: Introduce configurable expiration TTL (e.g. 5 minutes) and a `Clock` bean/parameter to ensure stale authorizations cannot execute.

4. **Approval Flow Hardening**:
   - For `APPROVAL_REQUIRED` actions, human supervisor approval must issue a **new** revalidated `AuthorizedAction` bound to the exact updated state and fingerprint. Any change to the proposal (parameters, target, type) must invalidate prior approvals.

5. **Tool Registry & Parameter Schema Validation**:
   - `McpToolDefinition` and `ToolRegistry` must enforce parameter schemas (required parameters, unexpected parameter rejection) *before* attempting MCP transport execution.

6. **Replay & Idempotency Key Mutation**:
   - Prevent executing identical actions repeatedly or using the same idempotency key with mutated parameters.

---

## 3. Baseline Audit Summary

| Area | Current Sprint 10 State | Target Sprint 10.1 Hardened State |
| :--- | :--- | :--- |
| `AuthorizedAction` Creation | Public constructor / `.of()` | Restricted factory + verified token/fingerprint |
| Action Fingerprinting | None (plain fields) | Canonical SHA-256 fingerprint binding all fields |
| Expiration TTL | None | Configurable TTL (`expiresAt` + Clock evaluation) |
| Parameter Validation | Basic non-null check | Schema-level type and required-key checks |
| Replay Protection | In-memory key caching | Fingerprint-verified idempotency protection |
| Approval Flow | Static decision enum | Revalidation lifecycle with fresh token issuance |
| Domain Purity | 100% clean of MCP/Spring AI | Maintained 100% pure technology-neutral |
