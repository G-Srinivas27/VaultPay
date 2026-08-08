package com.srinivas.vaultpay.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Base exception for all VaultPay application-level exceptions.
 *
 * <p>Why a custom base exception?
 * Instead of throwing generic {@code RuntimeException} everywhere and catching it
 * in a global handler with instanceof checks, we build a typed hierarchy.
 * Each exception carries its own HTTP status — no mapping logic needed elsewhere.
 *
 * <p>Design: extends RuntimeException (unchecked) so callers are NOT forced to
 * declare it in method signatures. This is the standard Spring Boot approach —
 * checked exceptions are a pain with lambda expressions and streams.
 */
public abstract class VaultPayException extends RuntimeException {

    private final HttpStatus status;

    protected VaultPayException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
