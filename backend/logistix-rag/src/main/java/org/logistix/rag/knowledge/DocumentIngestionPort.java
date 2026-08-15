package org.logistix.rag.knowledge;

import java.util.List;

/**
 * Inbound port for ingesting, chunking, and indexing knowledge documents.
 */
public interface DocumentIngestionPort {

    int ingestDocument(Document document);

    int ingestBatch(List<Document> documents);

    void removeDocument(String documentId);
}
