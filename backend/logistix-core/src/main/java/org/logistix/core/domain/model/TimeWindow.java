package org.logistix.core.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Immutable time window constraint for pickup or delivery.
 */
public record TimeWindow(Instant start, Instant end) {

    public TimeWindow {
        Objects.requireNonNull(start, "Start time must not be null");
        Objects.requireNonNull(end, "End time must not be null");
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("End time cannot be before start time");
        }
    }

    public boolean isWithin(Instant timestamp) {
        return !timestamp.isBefore(start) && !timestamp.isAfter(end);
    }
}
