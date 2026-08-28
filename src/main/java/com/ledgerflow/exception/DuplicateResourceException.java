package com.ledgerflow.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a business rule is violated.
 * Examples:
 *   - Trying to create an account with a duplicate account number
 *   - Trying to activate a closed branch
 *   - Trying to create a branch with a code that already exists
 * Results in HTTP 409 Conflict.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message) {
        super(message);
    }
}
