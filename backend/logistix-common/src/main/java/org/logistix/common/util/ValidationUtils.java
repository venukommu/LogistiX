package org.logistix.common.util;

import org.logistix.common.exception.ValidationException;

import java.util.Collection;
import java.util.Objects;

/**
 * Common validation utilities for domain value assertions.
 */
public final class ValidationUtils {

    private ValidationUtils() {
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
