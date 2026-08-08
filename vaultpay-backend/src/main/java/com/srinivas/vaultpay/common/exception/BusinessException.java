package com.srinivas.vaultpay.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a business rule is violated during an operation.
 *
 * <p>Examples:
 * <ul>
 *   <li>Attempting to withdraw more than the wallet balance</li>
 *   <li>Transferring funds to your own wallet</li>
 *   <li>Operating on an inactive/frozen account</li>
 * </ul>
 * Maps to HTTP 422 Unprocessable Entity — the request was well-formed
 * but semantically invalid from a business perspective.
 */
public class BusinessException extends VaultPayException {

    public BusinessException(String message) {
        super(message, HttpStatus.UNPROCESSABLE_ENTITY);
    }
}
