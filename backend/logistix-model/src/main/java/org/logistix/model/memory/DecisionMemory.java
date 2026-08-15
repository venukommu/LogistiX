package org.logistix.model.memory;

import java.util.List;
import java.util.Optional;

/**
 * Framework-level memory SPI supporting working memory and long-term recall
 * for agentic and contextual decision making.
 */
public interface DecisionMemory {

    void remember(String key, Object value, double relevance);

    Optional<MemoryEntry> retrieve(String key);

    void forget(String key);

    List<MemoryEntry> search(String query, int limit);

    String summarize();

    void clear();
}
