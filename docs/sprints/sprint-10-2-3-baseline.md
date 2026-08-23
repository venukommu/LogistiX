# Sprint 10.2.3 Baseline Audit: Security Configuration Consistency & Starter Decoupling

**Date**: August 23, 2026  
**Starting Commit**: `bc2a5a3`  
**Test Baseline**: 92 Tests Passing (0 Failures, 0 Errors, 0 Skipped across 14 modules)  

---

## 1. Current Dependency Graph & Starter Coupling

`mvn dependency:tree -pl :logistix-spring-boot-starter` reveals:
```
org.logistix:logistix-spring-boot-starter:jar:0.1.0-SNAPSHOT
+- org.logistix:logistix-dsl
+- org.logistix:logistix-engine
+- org.logistix:logistix-domain
+- org.logistix:logistix-common
+- org.logistix:logistix-ai
+- org.logistix:logistix-rag
+- org.logistix:logistix-mcp (MANDATORY DEPENDENCY)
+- org.springframework.boot:spring-boot-starter
```

### Why Starter Currently Depends on MCP:
In Sprint 10.2.2, `AuthorizationAuthorityRegistry` (located in `org.logistix.mcp`) was directly imported and auto-configured in `LogistiXAutoConfiguration`. Consequently, any Spring Boot application including `logistix-spring-boot-starter` is forced to pull `logistix-mcp`, violating technology neutrality and modular decoupling.

---

## 2. Security Configuration Model Audit

### 2.1 `issuerId` vs `authorityId` Semantics
- In `LogistiXProperties.AuthorizationSecurityProperties`:
  - `authorityId` defaults to `"LogistiX-Governance-Authority"`.
  - `issuerId` defaults to `"LogistiX-Governance-Authority"`.
  - Both properties exist, but `issuerId` is redundant and creates configuration ambiguity.
- In `DefaultActionAuthorizationIssuer`:
  - Parameter is named `issuerAuthorityId`.
- In `AuthorizationProvenance`:
  - Field is named `issuerAuthorityId`, with an alias method `issuerId()`.

**Resolution**: `authorityId` is the canonical authorization authority identity representing the trusted LogistiX governance authority. `issuerId` is removed or aliased with fail-fast validation against conflicting definitions.

### 2.2 `security.enabled` Semantics
- In `LogistiXProperties`:
  - `logistix.security.enabled` defaults to `true`.
- In `LogistiXAutoConfiguration`:
  - Security beans (`AuthorizationAuthorityRegistry`, `TrustedApproverRegistry`, `ActionAuthorizationIssuer`, `ActionApprovalIssuer`) were created unconditionally without checking `@ConditionalOnProperty(prefix = "logistix.security", name = "enabled", havingValue = "true", matchIfMissing = true)`.

**Resolution**: Guard security beans with `@ConditionalOnProperty(prefix = "logistix.security", name = "enabled", havingValue = "true", matchIfMissing = true)`. When `logistix.security.enabled=false`, security beans are not created and no wildcard identities are provisioned.

### 2.3 Default Approver Behavior
- In `LogistiXAutoConfiguration`:
  - If `logistix.security.approvers` is empty, it currently falls back to `TrustedApproverRegistry.withStandardLogisticsApprovers()`.
  - While convenient for demo examples, silently provisioning default trusted operational approvers in production code is unsafe.

**Resolution**: Follow Option A (Safest): When no approvers are configured, create an empty frozen `TrustedApproverRegistry` (no authorized approvers). Explicit approvers must be configured via `logistix.security.approvers` or explicit beans.

### 2.4 Startup Issuer Authority Consistency Validation
- The configured `authorityId` of `ActionAuthorizationIssuer` must be validated against `AuthorizationAuthorityRegistry` to ensure that the issuer authority is registered and active before freezing the registry.

---

## 3. Proposed Closure Architecture for Sprint 10.2.3

1. **Decouple Starter from MCP**:
   - Remove `logistix-mcp` dependency from `backend/logistix-spring-boot-starter/pom.xml`.
   - Remove `org.logistix.mcp.*` imports from `LogistiXAutoConfiguration`.
   - Create `org.logistix.mcp.autoconfig.LogistiXMcpAutoConfiguration` in `logistix-mcp` to auto-configure `AuthorizationAuthorityRegistry`, `ToolRegistry`, and `McpActionExecutor` conditionally when MCP is on the classpath.
2. **Canonicalize `authorityId` & Startup Validation**:
   - Clean up `LogistiXProperties.AuthorizationSecurityProperties`.
   - Validate that `authorityId` is present in `authorities` list before freezing. Fail fast if missing or conflicting.
3. **Enforce `security.enabled` Semantics**:
   - Conditionally register security beans only when `logistix.security.enabled` is `true`.
4. **Enforce Explicit Approvers**:
   - Default to empty frozen `TrustedApproverRegistry` when no approvers are declared.
5. **Context Tests**:
   - Test starter without MCP on classpath.
   - Test starter + MCP on classpath.
   - Test `security.enabled=false`.
   - Test startup validation failure for mismatched authority configuration.
