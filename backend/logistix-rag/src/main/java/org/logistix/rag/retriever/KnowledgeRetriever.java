package org.logistix.rag.retriever;

import java.util.Map;

/**
 * High-level knowledge retrieval interface for RAG pipelines.
 */
public interface KnowledgeRetriever {

    RetrievalContext retrieve(String query, int topK);

    RetrievalContext retrieveWithFilter(String query, int topK, Map<String, Object> filters);
}
