package org.logistix.rag.vectorstore;

import org.logistix.rag.embedding.Embedding;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Parameters for similarity search in a vector store.
 */
public record VectorSearchQuery(
        Embedding queryEmbedding,
        int topK,
        double minSimilarityScore,
        Map<String, Object> filterMetadata
) {
    public VectorSearchQuery {
        Objects.requireNonNull(queryEmbedding, "Query embedding cannot be null");
        if (topK <= 0) {
            throw new IllegalArgumentException("topK must be strictly positive");
        }
        filterMetadata = filterMetadata != null ? Map.copyOf(filterMetadata) : Collections.emptyMap();
    }

    public static VectorSearchQuery of(Embedding queryEmbedding, int topK) {
        return new VectorSearchQuery(queryEmbedding, topK, 0.0, Collections.emptyMap());
    }
}
