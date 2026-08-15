package org.logistix.common.exception;

/**
 * Thrown when an aggregate root or entity cannot be found by its identifier.
 */
public class EntityNotFoundException extends LogistixException {

    private final String entityType;
    private final String entityId;

    public EntityNotFoundException(String entityType, String entityId) {
        super(String.format("%s with identifier '%s' was not found.", entityType, entityId), "ENTITY_NOT_FOUND");
        this.entityType = entityType;
        this.entityId = entityId;
    }

    public String getEntityType() {
        return entityType;
    }

    public String getEntityId() {
        return entityId;
    }
}
