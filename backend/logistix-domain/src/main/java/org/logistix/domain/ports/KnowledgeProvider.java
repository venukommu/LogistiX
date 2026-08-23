package org.logistix.domain.ports;

import org.logistix.domain.decision.DecisionContext;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Outbound SPI for retrieving grounding enterprise domain knowledge (RAG).
 * Fully decoupled, framework-agnostic Java 21 interface.
 */
public interface KnowledgeProvider {

    /**
     * Strongly typed representation of retrieved grounding evidence with full provenance and versioning readiness.
     */
    record GroundingDocument(
            String documentId,
            String title,
            String content,
            String source,
            String section,
            String version,
            double relevanceScore,
            Map<String, String> metadata
    ) {
        public GroundingDocument {
            Objects.requireNonNull(documentId, "documentId must not be null");
            Objects.requireNonNull(title, "title must not be null");
            Objects.requireNonNull(content, "content must not be null");
            source = source != null ? source : "enterprise-knowledge-base";
            section = section != null ? section : "general";
            version = version != null ? version : "1.0";
            metadata = metadata != null ? Map.copyOf(metadata) : Collections.emptyMap();
        }

        /**
         * Backward-compatible 7-argument constructor.
         */
        public GroundingDocument(String documentId, String title, String content, String source, String section, double relevanceScore, Map<String, String> metadata) {
            this(documentId, title, content, source, section, "1.0", relevanceScore, metadata);
        }

        /**
         * Backward-compatible 4-argument constructor.
         */
        public GroundingDocument(String documentId, String title, String content, double relevanceScore) {
            this(documentId, title, content, "enterprise-knowledge-base", "general", "1.0", relevanceScore, Collections.emptyMap());
        }

        public static GroundingDocument of(String documentId, String title, String content, String source, String section, double relevanceScore) {
            return new GroundingDocument(documentId, title, content, source, section, "1.0", relevanceScore, Collections.emptyMap());
        }

        public static GroundingDocument of(String documentId, String title, String content, String source, String section, String version, double relevanceScore) {
            return new GroundingDocument(documentId, title, content, source, section, version, relevanceScore, Collections.emptyMap());
        }
    }

    /**
     * Structured query request for knowledge retrieval.
     */
    record KnowledgeQuery(
            String queryText,
            String decisionType,
            String domain,
            Set<String> requiredTopics,
            int maxResults
    ) {
        public KnowledgeQuery {
            queryText = queryText != null ? queryText : "";
            decisionType = decisionType != null ? decisionType : "GENERAL";
            domain = domain != null ? domain : "LOGISTICS";
            requiredTopics = requiredTopics != null ? Set.copyOf(requiredTopics) : Collections.emptySet();
            maxResults = Math.max(1, maxResults);
        }

        public static KnowledgeQuery of(String queryText, int maxResults) {
            return new KnowledgeQuery(queryText, "GENERAL", "LOGISTICS", Collections.emptySet(), maxResults);
        }

        public static KnowledgeQuery of(String queryText, String domain, int maxResults) {
            return new KnowledgeQuery(queryText, "GENERAL", domain, Collections.emptySet(), maxResults);
        }
    }

    /**
     * Returns the human-readable identifier of this knowledge provider.
     */
    default String getProviderName() {
        return "Default-Knowledge-Provider";
    }

    /**
     * Retrieves grounding evidence given an operational DecisionContext.
     */
    List<GroundingDocument> retrieveKnowledge(DecisionContext context, int maxResults);

    /**
     * Retrieves grounding evidence given a structured KnowledgeQuery.
     */
    default List<GroundingDocument> retrieveKnowledge(KnowledgeQuery query) {
        return Collections.emptyList();
    }
}
