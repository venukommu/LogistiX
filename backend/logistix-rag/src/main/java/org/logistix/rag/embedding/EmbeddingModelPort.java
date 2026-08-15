package org.logistix.rag.embedding;

import java.util.List;

/**
 * Port contract for generating text embeddings.
 */
public interface EmbeddingModelPort {

    Embedding embedText(String text);

    List<Embedding> embedBatch(List<String> texts);

    int getDimensions();
}
