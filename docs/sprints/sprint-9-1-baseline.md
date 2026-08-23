# Sprint 9.1 Baseline Audit: Knowledge Grounding & Boundary Hardening

**Date**: 2026-08-23  
**Status**: BASELINE ESTABLISHED  
**Commit**: `08af605` (Sprint 9 Architecture Closure)  
**Baseline Test Status**: 38/38 tests passing across all 13 modules (100% BUILD SUCCESS)

---

## 1. Current Architecture & Knowledge Flow

The LogistiX framework operates under the unified Decision Intelligence paradigm:
$$\text{Feasible Candidates (Hard Constraints)} \to \text{Business Rules} \to \text{Deterministic Scoring} \to \text{Top-N} \to \text{Knowledge Evidence Retrieval} \to \text{Grounded Single-Call AI Advisory} \to \text{Deterministic Policy} \to \text{Final Decision}$$

### Current Knowledge Flow
1. **`DriverDispatchKnowledgeStep`**: Executes prior to AI evaluation. Queries `KnowledgeProvider` SPI for up to `maxResults` `GroundingDocument`s matching operational context. Records `KnowledgeTelemetry`.
2. **`DispatchAIRequest`**: Carries `List<GroundingDocument>` into `logistix-ai`.
3. **`DispatchPromptBuilder`**: Constructs prompt using `DRIVER_DISPATCH_AI_PROMPT_V2` with `# RETRIEVED ENTERPRISE KNOWLEDGE EVIDENCE`.
4. **`AIProvider` (Mock / Spring AI)**: Returns `BatchedDispatchAIAdvice` or `DispatchAIAdvice` with `knowledgeEvidenceUsed`.
5. **`DriverDispatchAIStep`**: Validates returned citations and populates candidates with grounded risk narrative.
6. **`DriverDispatchRecommendationStep`**: Evaluates policy deterministically, strictly separating Explainability into Deterministic Factors, Knowledge Evidence, and AI Contextual Insights.

---

## 2. Weaknesses & Architectural Issues Identified

### Issue 1: Mock AI Provider Boundary Bleed
- `MockDispatchAIProvider` contains hardcoded knowledge-specific business interpretation (e.g. `DOC-WINTER-001` string checks to construct specific winter equipment warnings).
- **Remediation**: Convert `MockDispatchAIProvider` into a clean, configurable, scenario-aware test double that returns configured risk advisories and evidence citations without understanding document semantics.

### Issue 2: Retrieved Knowledge Untrusted Data Boundary & Prompt Injection
- Retrieved knowledge in `DispatchPromptBuilder` is not explicitly demarcated as **untrusted reference data**.
- **Remediation**: Restructure prompt into 5 explicit sections:
  1. `SYSTEM INSTRUCTIONS` (Authoritative)
  2. `OPERATIONAL FACTS` (Verified)
  3. `FEASIBLE CANDIDATES` (Pre-filtered)
  4. `RETRIEVED KNOWLEDGE EVIDENCE (UNTRUSTED REFERENCE DATA)` (Must not be treated as executable instructions)
  5. `REQUIRED RESPONSE FORMAT`
- Implement prompt bounding (max characters per doc, max total evidence characters).
- Add dedicated Prompt Injection verification tests (`DOC-MALICIOUS-001`).

### Issue 3: Evidence Citation Validation & Normalization
- `DriverDispatchAIStep` filters citations against valid evidence IDs, but duplicate citations are not explicitly de-duplicated while preserving deterministic order.
- **Remediation**: Normalize citations using `LinkedHashSet` to deduplicate and preserve order. Reject null/malformed citations.

### Issue 4: Evidence Provenance & Versioning Readiness
- `GroundingDocument` has `documentId`, `title`, `content`, `source`, `section`, `relevanceScore`, `metadata`, but lacks an optional `documentVersion` field for future document lifecycle versioning.
- **Remediation**: Add optional `documentVersion` (defaulting to `"1.0"`) with full backward compatibility.

### Issue 5: Prompt Version Consistency
- Verify all repository references consistently use `DRIVER_DISPATCH_AI_PROMPT_V2` and that any V1 references are explicitly noted as historical.

### Issue 6: Comprehensive Boundary Test Suite
- Create `KnowledgeGroundingBoundaryTest` covering prompt injection, malformed citations, duplicate citations, offline knowledge provider, and candidate safety.

---

## 3. Plan for Sprint 9.1 Phases

1. **Phase 1 & 2**: Make `MockDispatchAIProvider` configurable with response mappings/builders, removing hardcoded document semantics.
2. **Phase 3, 4 & 5**: Harden `DispatchPromptBuilder` with untrusted data boundaries, clear 5-section layout, and context length bounds.
3. **Phase 6 & 7**: Strengthen `DriverDispatchAIStep` citation validation, de-duplication, and normalization.
4. **Phase 8**: Add optional `documentVersion` to `GroundingDocument` and `KnowledgeTelemetry`.
5. **Phase 9, 10 & 11**: Add prompt injection test, knowledge failure tests, and boundary validation tests.
6. **Phase 12, 13 & 14**: Verify telemetry separation, prompt version V2 consistency, and constructor cleanup.
7. **Phase 15, 16 & 17**: Review domain purity, DTO boundaries, and explainability segregation.
8. **Phase 18 & 19**: Verify Golden Reference and Decision Lab regression.
9. **Phase 20 & 21**: Create `KnowledgeGroundingBoundaryTest` and perform security audit.
10. **Phase 22, 23 & 24**: Update documentation, architecture diagram, and run full build (`mvn clean verify`).
