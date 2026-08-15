package org.logistix.core.domain.model;

/**
 * Immutable spatial dimensions.
 */
public record Dimensions(double length, double width, double height, Unit unit) {

    public enum Unit {
        METERS,
        CENTIMETERS,
        FEET,
        INCHES
    }

    public Dimensions {
        if (length <= 0 || width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Dimensions must be strictly positive");
        }
    }

    public double volume() {
        return length * width * height;
    }
}
