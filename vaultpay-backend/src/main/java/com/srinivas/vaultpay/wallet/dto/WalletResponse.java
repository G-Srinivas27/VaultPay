package com.srinivas.vaultpay.wallet.dto;

import com.srinivas.vaultpay.wallet.entity.Wallet;
import com.srinivas.vaultpay.wallet.entity.WalletStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for wallet data returned in API responses.
 *
 * <p>Notice we expose the {@code userId} (a Long) rather than the full
 * {@code UserResponse} object. This is deliberate:
 * <ul>
 *   <li>Avoids deeply nested JSON that clients rarely need</li>
 *   <li>Prevents accidental data leakage (user fields we didn't intend to expose)</li>
 *   <li>Keeps the response payload small and fast to serialize</li>
 * </ul>
 * If a client needs full user details alongside wallet data, they make two
 * separate API calls — or we provide a dedicated "dashboard" endpoint later.
 *
 * @param id        the wallet's unique identifier
 * @param userId    the ID of the wallet's owner
 * @param balance   current wallet balance (exact decimal, never float/double)
 * @param currency  ISO 4217 currency code (e.g., "INR")
 * @param status    current wallet operational status
 * @param createdAt when this wallet was created
 */
public record WalletResponse(
        Long id,
        Long userId,
        BigDecimal balance,
        String currency,
        WalletStatus status,
        LocalDateTime createdAt
) {

    /**
     * Factory method to map a Wallet entity to a WalletResponse DTO.
     * Centralizes the mapping logic — one place to update if fields change.
     *
     * @param wallet the Wallet entity to map
     * @return a populated WalletResponse
     */
    public static WalletResponse from(Wallet wallet) {
        return new WalletResponse(
                wallet.getId(),
                wallet.getUser().getId(),   // Only the user's ID — not the full User object
                wallet.getBalance(),
                wallet.getCurrency(),
                wallet.getStatus(),
                wallet.getCreatedAt()
        );
    }
}
