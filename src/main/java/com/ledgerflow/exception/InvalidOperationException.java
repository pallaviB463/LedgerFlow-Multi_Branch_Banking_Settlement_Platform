package com.ledgerflow.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a banking operation violates business rules.
 * Examples:
 *   - Transferring from a FROZEN account
 *   - Transferring to a CLOSED account
 *   - Transferring to the same account as source
 * Results in HTTP 422 Unprocessable Entity.
 */
@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class InvalidOperationException extends RuntimeException {
    public InvalidOperationException(String message) {
        super(message);
    }
}
