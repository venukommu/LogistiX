# Sprint 10.2.1 Baseline Audit: Authorization Issuance Closure

**Date**: August 23, 2026  
**Starting Commit**: `8fd99b91d259c6bd239be3031eb2616767149872`  
**Test Baseline**: 77 Tests Passing (0 Failures, 0 Errors, 0 Skipped)  

---

## 1. Executive Summary

Sprint 10.2 successfully implemented tamper-evident canonical SHA-256 fingerprinting, exact-boundary expiration (`now >= expiresAt`), atomic idempotency, and reference provenance tracking.

However, an in-depth security inspection of commit `8fd99b9` identifies remaining authorization issuance and approval grant issuance vulnerabilities:
1. **`AuthorizedAction` Public Factory Methods & Backdoors**:
   - `AuthorizedAction.createForTesting(...)` exists in production `logistix-domain` code.
   - `AuthorizedAction.of(...)` and `AuthorizedAction.issue(...)` are public static methods on `AuthorizedAction`, allowing arbitrary application or malicious callers to mint `AuthorizedAction` instances outside of governance.
2. **`ActionApprovalGrant` Arbitrary Self-Issuance**:
   - `ActionApprovalGrant` has a public constructor and public static factories (`ActionApprovalGrant.of(...)`, `ActionApprovalGrant.forProposal(...)`).
   - Any caller can self-issue a grant with `approvedBy = "SUPERVISOR"` without validating against an authorized approver registry.
3. **McpActionExecutor Provenance Validation Weakness**:
   - Provenance validation in `McpActionExecutor` validates `provenance.isValid()` (checking format/prefix `PROV-LGX-` and length), rather than validating against a trusted in-process authorization authority/issuer registry.

---

## 2. Identified Creation & Validation Paths

### Current Authorization Creation Paths
- `AuthorizedAction.issue(ActionProposal, String, String, Duration, Instant)` — Public static method in domain.
- `AuthorizedAction.of(ActionProposal, String, String)` — Public static convenience method in domain.
- `AuthorizedAction.createForTesting(...)` — Public static backdoor in domain.

### Current Approval Creation Paths
- `new ActionApprovalGrant(...)` — Public constructor.
- `ActionApprovalGrant.of(...)` — Public static factory.
- `ActionApprovalGrant.forProposal(...)` — Public static factory allowing arbitrary strings for `approvedBy`.

### Current Provenance Validation
- `AuthorizationProvenance.isValid()` — Validates `provenanceToken.startsWith("PROV-LGX-") && provenanceToken.length() >= 16`.
- `McpActionExecutor.execute()` — Validates token starts with `AUTH-LGX-` and `provenance.isValid()`.

---

## 3. Proposed Closure Architecture for Sprint 10.2.1

1. **Trusted Authorization Authority & Issuer**:
   - Introduce `ActionAuthorizationIssuer` (or `AuthorizationIssuer`) interface and `DefaultActionAuthorizationIssuer` in `logistix-engine` / `logistix-domain`.
   - Make `AuthorizedAction` constructor package-private / restricted so only trusted issuance components can construct it.
   - Remove `createForTesting(...)` from `AuthorizedAction` entirely; provide `AuthorizedActionTestFactory` in `src/test/java`.
   - Remove `AuthorizedAction.of(...)` public factory.

2. **Trusted Approval Authority & Registry**:
   - Introduce `TrustedApproverRegistry` / `ApprovalAuthority` maintaining explicitly registered approver identities and roles.
   - Introduce `ActionApprovalIssuer` responsible for issuing `ActionApprovalGrant` with `ApprovalProvenance`.
   - Make `ActionApprovalGrant` constructor package-private / restricted, disallowing arbitrary self-issuance.
   - Provide `ActionApprovalGrantTestFactory` in `src/test/java` for test fixtures.

3. **In-Process Authority Provenance Verification**:
   - `McpActionExecutor` verifies `AuthorizationProvenance` against the trusted authorization authority registry.

---

## 4. Invariant Checklist

| Check | Baseline Status | Sprint 10.2.1 Target |
| :--- | :--- | :--- |
| `AuthorizedAction` public constructor | Closed (private) | Closed (package-private / restricted) |
| `createForTesting` in production code | **OPEN (Present in domain)** | **CLOSED (Removed to test sources)** |
| `AuthorizedAction.of` public bypass | **OPEN (Public)** | **CLOSED (Removed)** |
| `ActionApprovalGrant` self-issuance | **OPEN (Public constructor/factory)** | **CLOSED (Trusted ApprovalIssuer only)** |
| Approver identity validation | **OPEN (Arbitrary string accepted)** | **CLOSED (Validated against registry)** |
| In-process provenance verification | Prefix/shape check | Trusted Authority validation |
| Driver Dispatch regression | 100% Green | 100% Green |
| Decision Lab regression | 100% Green | 100% Green |
