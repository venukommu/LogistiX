package org.logistix.rag.vectorstore;

import org.logistix.rag.knowledge.DocumentChunk;

import java.util.Objects;

/**
 * Match result from vector similarity retrieval.
 */
public record VectorSearchResult(
        DocumentChunk chunk,
        double similarityScore
) {
    public VectorSearchResult {
        Objects.requireNonNull(chunk, "Chunk cannot be null");
    }
}
