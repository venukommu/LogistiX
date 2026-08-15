package org.logistix.common.exception;

/**
 * Root unchecked exception for all LogistiX domain and infrastructure errors.
 */
public abstract class LogistixException extends RuntimeException {

    private final String errorCode;

    protected LogistixException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    protected LogistixException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
