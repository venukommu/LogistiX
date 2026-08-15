package org.logistix.common.model;

/**
 * Standard pagination request parameters.
 */
public record PaginationRequest(int page, int size, String sortBy, String sortDirection) {

    public PaginationRequest {
        if (page < 0) {
            throw new IllegalArgumentException("Page index cannot be negative");
        }
        if (size <= 0 || size > 500) {
            throw new IllegalArgumentException("Page size must be between 1 and 500");
        }
        sortBy = (sortBy == null || sortBy.isBlank()) ? "id" : sortBy;
        sortDirection = (sortDirection == null || sortDirection.isBlank()) ? "ASC" : sortDirection.toUpperCase();
    }

    public static PaginationRequest of(int page, int size) {
        return new PaginationRequest(page, size, "id", "ASC");
    }

    public static PaginationRequest of(int page, int size, String sortBy, String sortDirection) {
        return new PaginationRequest(page, size, sortBy, sortDirection);
    }
}
