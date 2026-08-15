package org.logistix.domain.exceptions;

import org.logistix.common.exception.LogistixException;

/**
 * Base unchecked exception for all decision engine errors.
 */
public class DecisionException extends LogistixException {

    public DecisionException(String message) {
        super(message, "DECISION_ERROR");
    }

    public DecisionException(String message, String errorCode) {
        super(message, errorCode);
    }

    public DecisionException(String message, String errorCode, Throwable cause) {
        super(message, errorCode, cause);
    }
}
