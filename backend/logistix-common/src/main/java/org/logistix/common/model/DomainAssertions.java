package org.logistix.common.model;

import org.logistix.common.exception.ValidationException;

import java.util.Collection;

/**
 * Domain-oriented assertion utilities for validating invariant rules in immutable records and entities.
 */
public final class DomainAssertions {

    private DomainAssertions() {
        // Prevent instantiation
    }

    public static <T> T requireNonNull(T obj, String fieldName) {
        if (obj == null) {
            throw new ValidationException(fieldName + " must not be null");
        }
        return obj;
    }

    public static String requireNonBlank(String str, String fieldName) {
        if (str == null || str.trim().isEmpty()) {
            throw new ValidationException(fieldName + " must not be blank");
        }
        return str;
    }

    public static <T extends Collection<?>> T requireNotEmpty(T collection, String fieldName) {
        if (collection == null || collection.isEmpty()) {
            throw new ValidationException(fieldName + " must not be empty");
        }
        return collection;
    }

    public static double requirePositive(double value, String fieldName) {
        if (value <= 0) {
            throw new ValidationException(fieldName + " must be strictly positive");
        }
        return value;
    }
}
