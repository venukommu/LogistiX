package org.logistix.core.domain.model;

/**
 * Immutable weight measurement with unit.
 */
public record Weight(double value, Unit unit) {

    public enum Unit {
        KG,
        LBS,
        METRIC_TON
    }

    public Weight {
        if (value < 0) {
            throw new IllegalArgumentException("Weight value cannot be negative");
        }
    }

    public static Weight kilograms(double value) {
        return new Weight(value, Unit.KG);
    }

    public static Weight pounds(double value) {
        return new Weight(value, Unit.LBS);
    }
}
