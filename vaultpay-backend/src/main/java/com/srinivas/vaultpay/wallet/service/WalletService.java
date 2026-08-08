package com.srinivas.vaultpay.wallet.service;

import com.srinivas.vaultpay.wallet.dto.WalletResponse;

/**
 * Service interface defining the contract for wallet-related business operations.
 */
public interface WalletService {

    /**
     * Creates a new wallet for the specified user.
     *
     * <p>Business rules enforced:
     * <ul>
     *   <li>The user must exist — throws ResourceNotFoundException if not</li>
     *   <li>One wallet per user — throws BusinessException if wallet already exists</li>
     *   <li>New wallets start with a zero balance</li>
     *   <li>Default currency is INR</li>
     * </ul>
     *
     * @param userId the ID of the user to create a wallet for
     * @return the newly created wallet
     */
    WalletResponse createWallet(Long userId);

    /**
     * Retrieves the wallet belonging to a specific user.
     *
     * @param userId the user's ID
     * @return the user's wallet
     * @throws com.srinivas.vaultpay.common.exception.ResourceNotFoundException if no wallet exists for this user
     */
    WalletResponse getWalletByUserId(Long userId);

    /**
     * Retrieves a wallet by its own ID.
     *
     * @param walletId the wallet's ID
     * @return the wallet
     * @throws com.srinivas.vaultpay.common.exception.ResourceNotFoundException if wallet not found
     */
    WalletResponse getWalletById(Long walletId);
}
