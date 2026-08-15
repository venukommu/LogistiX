package org.logistix.model.metadata;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable metadata characterizing a DecisionModel, including authorship, versioning,
 * checksum integrity, and semantic classification tags.
 */
public record ModelMetadata(
        String version,
        List<String> tags,
        String author,
        String description,
        Instant createdAt,
        String checksum,
        Map<String, Object> customAttributes
) {
    public ModelMetadata {
        version = version != null ? version : "1.0.0";
        tags = tags != null ? List.copyOf(tags) : Collections.emptyList();
        author = author != null ? author : "system";
        description = description != null ? description : "";
        createdAt = createdAt != null ? createdAt : Instant.now();
        checksum = checksum != null ? checksum : "";
        customAttributes = customAttributes != null ? Map.copyOf(customAttributes) : Collections.emptyMap();
    }

    public static ModelMetadata simple(String version, String description) {
        return new ModelMetadata(version, Collections.emptyList(), "system", description, Instant.now(), "", Collections.emptyMap());
    }

    public static ModelMetadata of(String version, List<String> tags, String author, String description) {
        return new ModelMetadata(version, tags, author, description, Instant.now(), "", Collections.emptyMap());
    }
}
