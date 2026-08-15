package org.logistix.common.util;

import org.logistix.common.model.DomainAssertions;

import java.util.Collection;

/**
 * Validation utility facade.
 *
 * @deprecated Prefer using {@link DomainAssertions} for explicit domain-oriented assertion semantics.
 */
@Deprecated(since = "0.1.0", forRemoval = false)
public final class ValidationUtils {

    private ValidationUtils() {
        // Prevent instantiation
    }

    public static <T> T requireNonNull(T obj, String fieldName) {
        return DomainAssertions.requireNonNull(obj, fieldName);
    }

    public static String requireNonBlank(String str, String fieldName) {
        return DomainAssertions.requireNonBlank(str, fieldName);
    }

    public static <T extends Collection<?>> T requireNotEmpty(T collection, String fieldName) {
        return DomainAssertions.requireNotEmpty(collection, fieldName);
    }

    public static double requirePositive(double value, String fieldName) {
        return DomainAssertions.requirePositive(value, fieldName);
    }
}
