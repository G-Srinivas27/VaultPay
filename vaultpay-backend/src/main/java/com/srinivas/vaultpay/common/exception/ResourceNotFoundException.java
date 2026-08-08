package com.srinivas.vaultpay.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a requested resource does not exist in the system.
 *
 * <p>Examples: User not found by ID, Wallet not found for a user.
 * Maps to HTTP 404 Not Found.
 */
public class ResourceNotFoundException extends VaultPayException {

    public ResourceNotFoundException(String resource, String field, Object value) {
        super(String.format("%s not found with %s: '%s'", resource, field, value), HttpStatus.NOT_FOUND);
    }
}
