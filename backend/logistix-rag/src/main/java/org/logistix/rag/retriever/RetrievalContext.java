package org.logistix.rag.retriever;

import org.logistix.rag.vectorstore.VectorSearchResult;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Contextual grounding data retrieved for LLM prompt augmentation.
 */
public record RetrievalContext(
        String query,
        List<VectorSearchResult> results,
        String formattedContext
) {
    public RetrievalContext {
        Objects.requireNonNull(query, "Query cannot be null");
        results = results != null ? List.copyOf(results) : Collections.emptyList();
    }
}
