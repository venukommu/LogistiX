package org.logistix.domain.fact;

/**
 * Origin and lineage of a domain fact.
 */
public enum FactSource {
    SYSTEM,
    SENSOR,
    TELEMETRY,
    USER,
    INFERRED,
    EXTERNAL,
    AI_MODEL,
    KNOWLEDGE_BASE
}
