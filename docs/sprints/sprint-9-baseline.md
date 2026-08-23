# Sprint 9 Baseline Report: Knowledge-Aware Decision Intelligence

## 1. Executive Summary
Following the completion of Sprint 8.1 (commit `3b53c9c`), the Driver Dispatch reference capability and Decision Lab are closed as the Golden Reference Implementation.

**Sprint 9** introduces Knowledge-Aware Decision Intelligence into LogistiX, exploring:
> *"What happens when AI has access to grounded enterprise knowledge before providing a decision advisory?"*

The central architectural principle remains:
```
"The deterministic engine establishes what is feasible.
 Enterprise Knowledge provides evidence.
 AI contextual advisory interprets evidence.
 A deterministic policy evaluates the advisory.
 LogistiX retains authority over the final decision."
```

---

## 2. Review of Existing Architecture & SPIs

### 2.1 Existing `KnowledgeProvider` SPI
In `logistix-domain` (`org.logistix.domain.ports.KnowledgeProvider`):
- `List<GroundingDocument> retrieveKnowledge(DecisionContext context, int maxResults)`
- `record GroundingDocument(String documentId, String title, String content, double relevanceScore)`

**Evaluation**:
The existing SPI in `logistix-domain` is clean, lightweight, and pure Java 21 (0 Spring / external dependencies).
We will retain and refine `KnowledgeProvider` without introducing any competing SPI:
- Enrich `GroundingDocument` with provenance fields (`source`, `section`, `metadata`) via backward-compatible constructors.
- Add structured `KnowledgeQuery` and `getProviderName()` default methods.

### 2.2 Existing `AIProvider` SPI
In `logistix-domain` (`org.logistix.domain.ports.AIProvider`):
- `infer(DecisionContext context, Class<T> responseType)`
- `generateReasoning(DecisionContext context, Object candidate)`

**Evaluation**:
The SPI is provider-agnostic. In Sprint 9, `DispatchAIRequest` in `logistix-ai` will include `List<GroundingDocument> knowledgeEvidence` to feed retrieved knowledge to the AI prompt cleanly.

### 2.3 Proposed Knowledge Flow in Decision Pipeline
```
Deterministic Feasibility -> Soft Rules -> Deterministic Scoring (Top-N)
       │
       ▼
Knowledge Retrieval (KnowledgeProvider SPI -> InMemoryKnowledgeProvider)
       │  (Returns Grounding Evidence e.g. DOC-WINTER-001)
       ▼
Grounded AI Advisory (AIProvider SPI -> SpringAI / MockAI)
       │  (Evaluates candidate facts + knowledge evidence, cites evidence IDs)
       ▼
Deterministic Policy Evaluation (DriverDispatchRecommendationStep)
       │  (Evaluates verified qualitative signals against deterministic safety)
       ▼
Auditable Recommendation & Explainability (Separating Deterministic Factors, Knowledge Evidence, AI Insights)
```

---

## 3. Current Limitations Addressed in Sprint 9
1. **Un-grounded AI Context**: Previously, AI only evaluated immediate prompt attributes (e.g. `weatherAdvisory: BLIZZARD`). Enterprise operating procedures, corridor guidelines, and safety policies were not formally accessible.
2. **Provenance & Evidence**: No formal trace of *why* a particular guideline applies to a corridor.
3. **Knowledge vs. No-Knowledge Comparison**: Need to demonstrate the empirical difference between:
   - `RULES_ONLY` (0 AI, 0 Knowledge)
   - `HYBRID_AI` (1 AI call, No Knowledge)
   - `KNOWLEDGE_AI` (1 Knowledge retrieval + 1 Grounded AI call)

---

## 4. Verification Check
Running baseline `mvn clean test` to confirm 100% clean initial build.
