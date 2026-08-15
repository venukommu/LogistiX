package org.logistix.rag.knowledge;

import org.logistix.rag.embedding.Embedding;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * An individual segmented chunk of a document with associated embedding vector.
 */
public record DocumentChunk(
        String id,
        String documentId,
        int chunkIndex,
        String content,
        Embedding embedding,
        Map<String, Object> metadata
) {
    public DocumentChunk {
        Objects.requireNonNull(id, "Chunk ID must not be null");
        Objects.requireNonNull(documentId, "Document ID must not be null");
        Objects.requireNonNull(content, "Content must not be null");
        metadata = metadata != null ? Map.copyOf(metadata) : Collections.emptyMap();
    }
}
