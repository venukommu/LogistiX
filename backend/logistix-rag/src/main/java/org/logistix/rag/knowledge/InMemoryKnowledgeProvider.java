package org.logistix.rag.knowledge;

import org.logistix.domain.decision.DecisionContext;
import org.logistix.domain.ports.KnowledgeProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Fast, deterministic in-memory knowledge provider implementing the KnowledgeProvider SPI.
 * Requires zero external infrastructure, vector databases, or network connections.
 */
public class InMemoryKnowledgeProvider implements KnowledgeProvider {

    private final String providerName;
    private final boolean simulatedOffline;
    private final Map<String, GroundingDocument> documentStore = new ConcurrentHashMap<>();

    public InMemoryKnowledgeProvider() {
        this("InMemory-Enterprise-Knowledge-Provider", false);
        loadDefaultReferenceKnowledge();
    }

    public InMemoryKnowledgeProvider(String providerName, boolean simulatedOffline) {
        this.providerName = providerName != null ? providerName : "InMemory-Enterprise-Knowledge-Provider";
        this.simulatedOffline = simulatedOffline;
    }

    public static InMemoryKnowledgeProvider withDefaults() {
        InMemoryKnowledgeProvider provider = new InMemoryKnowledgeProvider("InMemory-Default-Knowledge", false);
        provider.loadDefaultReferenceKnowledge();
        return provider;
    }

    public static InMemoryKnowledgeProvider empty() {
        return new InMemoryKnowledgeProvider("InMemory-Empty-Knowledge", false);
    }

    public static InMemoryKnowledgeProvider offline() {
        return new InMemoryKnowledgeProvider("InMemory-Offline-Knowledge", true);
    }

    @Override
    public String getProviderName() {
        return providerName;
    }

    public void registerDocument(GroundingDocument document) {
        Objects.requireNonNull(document, "document must not be null");
        documentStore.put(document.documentId(), document);
    }

    public void clear() {
        documentStore.clear();
    }

    @Override
    public List<GroundingDocument> retrieveKnowledge(DecisionContext context, int maxResults) {
        if (simulatedOffline) {
            throw new RuntimeException("Knowledge provider offline (simulated connection outage)");
        }

        String weather = context.getEnvironmentAttribute("weatherAdvisory", String.class).orElse("");
        String traffic = context.getEnvironmentAttribute("trafficRiskLevel", String.class).orElse("");
        String notes = context.getEnvironmentAttribute("corridorNotes", String.class).orElse("");

        String queryText = (weather + " " + traffic + " " + notes + " " + context.decisionType()).trim();
        return retrieveKnowledge(new KnowledgeQuery(queryText, context.decisionType(), "LOGISTICS", Collections.emptySet(), maxResults));
    }

    @Override
    public List<GroundingDocument> retrieveKnowledge(KnowledgeQuery query) {
        if (simulatedOffline) {
            throw new RuntimeException("Knowledge provider offline (simulated connection outage)");
        }

        if (documentStore.isEmpty() || query == null || query.maxResults() <= 0) {
            return Collections.emptyList();
        }

        Set<String> queryTokens = tokenize(query.queryText());
        List<ScoredDoc> scoredDocs = new ArrayList<>();

        for (GroundingDocument doc : documentStore.values()) {
            double score = computeRelevance(doc, queryTokens, query.requiredTopics());
            if (score > 0.15) {
                // Return GroundingDocument with updated query-specific relevance
                scoredDocs.add(new ScoredDoc(
                        new GroundingDocument(
                                doc.documentId(),
                                doc.title(),
                                doc.content(),
                                doc.source(),
                                doc.section(),
                                Math.round(score * 100.0) / 100.0,
                                doc.metadata()
                        ),
                        score
                ));
            }
        }

        return scoredDocs.stream()
                .sorted(Comparator.comparingDouble(ScoredDoc::score).reversed())
                .limit(query.maxResults())
                .map(ScoredDoc::doc)
                .collect(Collectors.toList());
    }

    private double computeRelevance(GroundingDocument doc, Set<String> queryTokens, Set<String> requiredTopics) {
        if (queryTokens.isEmpty()) {
            return doc.relevanceScore();
        }

        Set<String> docTokens = tokenize(doc.title() + " " + doc.section() + " " + doc.content());
        long matchCount = queryTokens.stream().filter(docTokens::contains).count();
        double matchRatio = (double) matchCount / Math.max(queryTokens.size(), 1);

        double topicBonus = 0.0;
        if (!requiredTopics.isEmpty()) {
            long topicMatches = requiredTopics.stream()
                    .filter(t -> docTokens.contains(t.toLowerCase()) || doc.metadata().containsKey(t))
                    .count();
            topicBonus = 0.25 * ((double) topicMatches / requiredTopics.size());
        }

        return Math.min(1.0, (matchRatio * 0.70) + topicBonus + (doc.relevanceScore() * 0.20));
    }

    private Set<String> tokenize(String text) {
        if (text == null || text.isBlank()) {
            return Collections.emptySet();
        }
        return Arrays.stream(text.toLowerCase().split("[^a-zA-Z0-9_-]+"))
                .filter(s -> s.length() > 2)
                .collect(Collectors.toSet());
    }

    private void loadDefaultReferenceKnowledge() {
        registerDocument(new GroundingDocument(
                "DOC-WINTER-001",
                "Winter Operations & Severe Corridor Guidelines",
                "Vehicles operating through mountain passes (such as Donner Pass) under Blizzard or Severe Winter Storm warnings must meet certified winter equipment readiness. Standard tier drivers without winter endorsements face chain inspection delays exceeding 180 minutes. Priority must be given to drivers with verified winter certifications and robust HOS safety buffer.",
                "enterprise-safety-manual-v3.md",
                "Section 4.2 - Mountain Corridor Severe Weather Policy",
                0.95,
                Map.of("category", "WINTER_SAFETY", "corridor", "DONNER_PASS")
        ));

        registerDocument(new GroundingDocument(
                "DOC-PHARMA-002",
                "Cold Chain & Sensitive Goods Handling Standards",
                "High-priority pharmaceutical and temperature-sensitive shipments require certified drivers with active TWIC endorsements, continuous temperature telemetry, and a minimum 3-hour HOS safety buffer.",
                "enterprise-pharma-compliance.md",
                "Section 2.1 - Cold Chain Integrity & Security",
                0.90,
                Map.of("category", "COLD_CHAIN", "priority", "CRITICAL")
        ));

        registerDocument(new GroundingDocument(
                "DOC-HAZMAT-003",
                "Hazardous Materials Transport Regulatory Policy",
                "Drivers assigned to HazMat loads must hold active federal HazMat endorsements, carry emergency spill response kits, and verify placarding at pickup. Missing certifications result in immediate deterministic disqualification.",
                "hazmat-dot-compliance.md",
                "Section 1.0 - Mandatory Endorsements",
                0.92,
                Map.of("category", "HAZMAT", "compliance", "MANDATORY")
        ));

        registerDocument(new GroundingDocument(
                "DOC-ROUTE-004",
                "Regional Routing & Carrier Performance Optimization",
                "US-West corridors (I-80 and I-5) experience heavy seasonal bottleneck delays. Fleet optimization favors drivers with historical on-time delivery rates above 95% and Platinum loyalty status.",
                "regional-routing-guidance.md",
                "Section 3.5 - US-West Congestion Management",
                0.85,
                Map.of("category", "ROUTING", "region", "US-WEST")
        ));
    }

    private record ScoredDoc(GroundingDocument doc, double score) {}
}
