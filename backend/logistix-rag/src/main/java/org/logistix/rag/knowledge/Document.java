package org.logistix.rag.knowledge;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable source knowledge document representation.
 */
public record Document(
        String id,
        String title,
        String content,
        String sourceUri,
        Map<String, Object> metadata
) {
    public Document {
        Objects.requireNonNull(id, "Document ID must not be null");
        Objects.requireNonNull(content, "Document content must not be null");
        metadata = metadata != null ? Map.copyOf(metadata) : Collections.emptyMap();
    }

    public static Document of(String id, String title, String content, String sourceUri) {
        return new Document(id, title, content, sourceUri, Collections.emptyMap());
    }
}
