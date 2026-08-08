package com.srinivas.vaultpay.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when an attempt is made to create a resource that already exists.
 *
 * <p>Example: Registering with an email that is already taken.
 * Maps to HTTP 409 Conflict.
 */
public class DuplicateResourceException extends VaultPayException {

    public DuplicateResourceException(String resource, String field, Object value) {
        super(String.format("%s already exists with %s: '%s'", resource, field, value), HttpStatus.CONFLICT);
    }
}
