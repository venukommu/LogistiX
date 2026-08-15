package org.logistix.core.domain.model;

import org.logistix.common.model.Coordinates;

/**
 * Immutable geographic location with address and coordinates.
 */
public record Location(
        String addressLine1,
        String city,
        String stateOrProvince,
        String postalCode,
        String countryCode,
        Coordinates coordinates
) {
    public static Location of(String city, String countryCode, Coordinates coordinates) {
        return new Location(null, city, null, null, countryCode, coordinates);
    }
}
