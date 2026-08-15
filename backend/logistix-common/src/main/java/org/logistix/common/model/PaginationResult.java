package org.logistix.common.model;

import java.util.Collections;
import java.util.List;

/**
 * Standard paginated container response.
 *
 * @param <T> Item content type
 */
public record PaginationResult<T>(
        List<T> items,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext,
        boolean hasPrevious
) {
    public PaginationResult {
        items = items != null ? List.copyOf(items) : Collections.emptyList();
    }

    public static <T> PaginationResult<T> of(List<T> items, int page, int size, long totalElements) {
        int totalPages = size == 0 ? 1 : (int) Math.ceil((double) totalElements / (double) size);
        boolean hasNext = page < (totalPages - 1);
        boolean hasPrevious = page > 0;
        return new PaginationResult<>(items, page, size, totalElements, totalPages, hasNext, hasPrevious);
    }
}
