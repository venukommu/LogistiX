package org.logistix.common.exception;

/**
 * Thrown when a business domain rule or invariant is violated.
 */
public class DomainException extends LogistixException {

    public DomainException(String message) {
        super(message, "DOMAIN_ERROR");
    }

    public DomainException(String message, String errorCode) {
        super(message, errorCode);
    }

    public DomainException(String message, String errorCode, Throwable cause) {
        super(message, errorCode, cause);
    }
}
