package org.logistix.rag.embedding;

import java.util.Arrays;
import java.util.Objects;

/**
 * Immutable vector embedding representation.
 */
public record Embedding(float[] vector, int dimensions) {

    public Embedding {
        Objects.requireNonNull(vector, "Vector cannot be null");
        if (vector.length == 0 || vector.length != dimensions) {
            throw new IllegalArgumentException("Vector length does not match specified dimensions: " + dimensions);
        }
        vector = Arrays.copyOf(vector, vector.length);
    }

    public static Embedding of(float[] vector) {
        return new Embedding(vector, vector.length);
    }
}
