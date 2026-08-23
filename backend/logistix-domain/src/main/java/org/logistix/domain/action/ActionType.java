package org.logistix.domain.action;

import java.util.Objects;

/**
 * Technology-neutral domain entity representing the type or operation of an enterprise action.
 */
public record ActionType(String code, String description) {

    public ActionType {
        Objects.requireNonNull(code, "Action code must not be null");
        code = code.trim().toUpperCase();
        description = description != null ? description : code;
    }

    public static final ActionType CHANGE_DELIVERY_APPOINTMENT =
            new ActionType("CHANGE_DELIVERY_APPOINTMENT", "Reschedule delivery appointment window");

    public static final ActionType ASSIGN_DRIVER =
            new ActionType("ASSIGN_DRIVER", "Assign driver to shipment order");

    public static final ActionType UPDATE_SHIPMENT_STATUS =
            new ActionType("UPDATE_SHIPMENT_STATUS", "Update operational status of a shipment");

    public static final ActionType REROUTE_SHIPMENT =
            new ActionType("REROUTE_SHIPMENT", "Reroute shipment to alternative corridor");

    public static final ActionType CANCEL_SHIPMENT =
            new ActionType("CANCEL_SHIPMENT", "Cancel shipment order");

    public static ActionType of(String code) {
        return new ActionType(code, code);
    }

    public static ActionType of(String code, String description) {
        return new ActionType(code, description);
    }
}
