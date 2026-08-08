package com.srinivas.vaultpay.wallet.repository;

import com.srinivas.vaultpay.wallet.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Data access layer for the Wallet entity.
 *
 * <p><b>findByUserId vs findByUser:</b>
 * Spring Data understands nested property traversal.
 * "findBy" + "User" + "Id" → navigates Wallet → user → id field
 * → generates: SELECT * FROM wallets WHERE user_id = ?
 *
 * <p>This is cleaner than {@code findByUser(User user)} because the caller
 * only needs to pass a Long (user ID), not fetch a full User object first.
 *
 * <p><b>existsByUserId:</b>
 * Used to check if a wallet already exists for a user before creating one.
 * Prevents duplicate wallets at the application layer.
 */
public interface WalletRepository extends JpaRepository<Wallet, Long> {

    /**
     * Finds the wallet belonging to a specific user.
     *
     * @param userId the ID of the user
     * @return the user's wallet if it exists
     */
    Optional<Wallet> findByUserId(Long userId);

    /**
     * Checks whether a wallet already exists for a given user.
     * Used to prevent duplicate wallet creation.
     *
     * @param userId the ID of the user
     * @return true if the user already has a wallet
     */
    boolean existsByUserId(Long userId);
}
