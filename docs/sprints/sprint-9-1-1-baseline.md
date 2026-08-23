# Sprint 9.1.1 Baseline Audit: Mock AI & Knowledge Boundary Final Hardening

**Date**: 2026-08-23  
**Status**: BASELINE ESTABLISHED  
**Commit**: `37d0e38` (Sprint 9.1 Architecture Closure)  
**Baseline Test Status**: 47/47 tests passing across all 13 modules (100% BUILD SUCCESS)

---

## 1. Current State & Identified Remaining Heuristics

### Identified Issue in `MockDispatchAIProvider.java`
Lines 74-104 in `MockDispatchAIProvider.java` currently retain fallback decision heuristics:
```java
if (weather.contains("BLIZZARD") || weather.contains("STORM")) {
    if ("PLATINUM".equalsIgnoreCase(c.driverTier()) || c.driverRating() >= 4.9) {
        risk = RiskLevel.LOW;
    } else {
        risk = RiskLevel.HIGH;
    }
} else if (weather.contains("RAIN")) {
    risk = RiskLevel.MEDIUM;
} else {
    risk = RiskLevel.LOW;
}
```
These are domain decision rules that simulate engine logic rather than acting as a pure, configurable test double.

---

## 2. Target Architecture for Sprint 9.1.1

1. **Pure Configurable Test Double (`MockDispatchAIProvider`)**:
   - **Zero** weather checks (`BLIZZARD`, `STORM`, `RAIN`).
   - **Zero** driver attribute checks (`tier`, `rating`, `HOS`).
   - **Zero** knowledge document semantics interpretation.
   - Default unconfigured behavior returns a deterministic, neutral advisory (`RiskLevel.LOW`, 0.85 confidence, "Neutral mock contextual advisory.", empty warnings/evidence).
   - Configured behavior returns explicit test/scenario responses mapped by candidate ID.

2. **Scenario Configuration in Decision Lab**:
   - For Decision Lab scenarios demonstrating AI differentiation (e.g. Scenario 4 & Scenario 5), tests or comparison engine configure explicit candidate advisories:
     - Driver A (Sam): `RiskLevel.HIGH`, 0.92 conf, "Elevated transit risk.", cited evidence.
     - Driver B (Elena): `RiskLevel.LOW`, 0.95 conf, "Corridor readiness verified.", cited evidence.

3. **Knowledge Remains Untrusted Reference Data**:
   - Prompt structure (4 sections), untrusted data warnings, context bounds, and injection neutralization remain strictly verified.

4. **Multi-Module Full Verification**:
   - All Golden Reference, Decision Lab, Knowledge Grounding, and boundary tests pass 100%.
