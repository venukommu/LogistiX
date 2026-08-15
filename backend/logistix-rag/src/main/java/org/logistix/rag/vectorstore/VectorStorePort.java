package org.logistix.rag.vectorstore;

import org.logistix.rag.knowledge.DocumentChunk;

import java.util.List;

/**
 * Outbound SPI for vector database operations (pgvector).
 */
public interface VectorStorePort {

    void save(DocumentChunk chunk);

    void saveAll(List<DocumentChunk> chunks);

    List<VectorSearchResult> search(VectorSearchQuery query);

    void deleteByDocumentId(String documentId);
}
