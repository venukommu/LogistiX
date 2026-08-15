package org.logistix.domain.ports;

import org.logistix.domain.decision.DecisionContext;

import java.util.List;

/**
 * Outbound SPI for retrieving grounding enterprise domain knowledge (RAG).
 */
public interface KnowledgeProvider {

    record GroundingDocument(
            String documentId,
            String title,
            String content,
            double relevanceScore
    ) {}

    List<GroundingDocument> retrieveKnowledge(DecisionContext context, int maxResults);
}
