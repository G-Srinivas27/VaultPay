package com.srinivas.vaultpay.user.entity;

/**
 * Represents the role of a user within VaultPay.
 *
 * <p>Why an enum?
 * Roles are a fixed, finite set of values. Using an enum instead of a plain String
 * prevents typos like "ADMINN" from slipping through — the compiler enforces valid values.
 *
 * <p>Spring Security will use these roles for authorization decisions (e.g., only
 * ADMIN can access the admin dashboard).
 */
public enum Role {
    USER,   // Standard customer — can manage their own wallet and transactions
    ADMIN   // Platform administrator — can view all users and transactions
}
