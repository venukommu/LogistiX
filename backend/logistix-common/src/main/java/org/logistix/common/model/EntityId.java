package org.logistix.common.model;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * Strongly typed, immutable identifier wrapper for domain entities.
 *
 * @param <T> The type of the underlying identifier value
 */
public record EntityId<T extends Serializable>(T value) implements Serializable {

    public EntityId {
        Objects.requireNonNull(value, "Entity identifier value must not be null");
    }

    public static EntityId<UUID> of(UUID value) {
        return new EntityId<>(value);
    }

    public static EntityId<String> of(String value) {
        return new EntityId<>(value);
    }

    public static EntityId<UUID> random() {
        return new EntityId<>(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
