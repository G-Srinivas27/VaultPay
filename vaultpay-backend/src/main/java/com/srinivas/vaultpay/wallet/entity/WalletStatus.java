package com.srinivas.vaultpay.wallet.entity;

/**
 * Represents the current operational status of a wallet.
 *
 * <p>Why model status as an enum instead of a boolean 'active' flag?
 * A boolean can only say "active" or "inactive". But wallets in real systems
 * have richer states:
 * <ul>
 *   <li>ACTIVE  — fully operational, can send and receive funds</li>
 *   <li>FROZEN  — temporarily locked (e.g., fraud investigation), no transactions allowed</li>
 *   <li>CLOSED  — permanently deactivated, cannot be reopened</li>
 * </ul>
 * An enum makes these states explicit and extensible. Adding a new state
 * later (e.g., SUSPENDED) requires zero changes to any existing logic.
 */
public enum WalletStatus {
    ACTIVE,   // Normal operation
    FROZEN,   // Temporarily locked — no debits or credits allowed
    CLOSED    // Permanently closed — terminal state
}
