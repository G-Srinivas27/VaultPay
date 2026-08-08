package com.srinivas.vaultpay.wallet.service;

import com.srinivas.vaultpay.common.exception.BusinessException;
import com.srinivas.vaultpay.common.exception.ResourceNotFoundException;
import com.srinivas.vaultpay.user.entity.User;
import com.srinivas.vaultpay.user.repository.UserRepository;
import com.srinivas.vaultpay.wallet.dto.WalletResponse;
import com.srinivas.vaultpay.wallet.entity.Wallet;
import com.srinivas.vaultpay.wallet.repository.WalletRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of {@link WalletService} containing wallet business logic.
 *
 * <p><b>Cross-module dependency — UserRepository:</b>
 * WalletServiceImpl depends on {@link UserRepository} to verify a user exists
 * before creating their wallet. This is an intentional cross-module dependency —
 * we need to validate the userId before creating the wallet.
 *
 * <p>In a microservices architecture, this would be an HTTP call to the User Service.
 * In our modular monolith, a direct repository reference is acceptable and efficient.
 *
 * <p><b>Why not inject UserService instead of UserRepository?</b>
 * We only need to FIND a User entity — we don't need any business logic from
 * UserService. Injecting the repository directly is more efficient (no extra
 * abstraction layer for a simple lookup) and avoids potential circular bean
 * dependencies between services.
 */
@Service
public class WalletServiceImpl implements WalletService {

    private static final Logger log = LoggerFactory.getLogger(WalletServiceImpl.class);

    private final WalletRepository walletRepository;
    private final UserRepository userRepository;

    public WalletServiceImpl(WalletRepository walletRepository, UserRepository userRepository) {
        this.walletRepository = walletRepository;
        this.userRepository = userRepository;
    }

    /**
     * Creates a wallet for a user.
     *
     * <p>Flow:
     * 1. Verify user exists — fail fast if not (ResourceNotFoundException)
     * 2. Check no wallet already exists — fail fast if so (BusinessException)
     * 3. Build and persist new wallet with zero balance
     * 4. Return WalletResponse DTO
     *
     * <p><b>@Transactional:</b>
     * The existence check and the save must happen atomically. Without a
     * transaction, two simultaneous requests for the same user could both
     * pass the existence check and create duplicate wallets. The transaction
     * + DB unique constraint on user_id provide two layers of protection.
     */
    @Override
    @Transactional
    public WalletResponse createWallet(Long userId) {
        log.debug("Creating wallet for user ID: {}", userId);

        // Step 1: Verify the user exists
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // Step 2: One wallet per user — enforce at application layer
        if (walletRepository.existsByUserId(userId)) {
            throw new BusinessException(
                    "User with ID " + userId + " already has a wallet. Only one wallet per user is allowed."
            );
        }

        // Step 3: Build wallet — balance=ZERO and status=ACTIVE by @Builder.Default
        Wallet wallet = Wallet.builder()
                .user(user)
                .build();

        Wallet savedWallet = walletRepository.save(wallet);
        log.info("Wallet created successfully with ID: {} for user ID: {}", savedWallet.getId(), userId);

        return WalletResponse.from(savedWallet);
    }

    /**
     * Retrieves a wallet by the owner's user ID.
     * Most clients look up wallets by user ID (e.g., "show MY wallet").
     */
    @Override
    @Transactional(readOnly = true)
    public WalletResponse getWalletByUserId(Long userId) {
        log.debug("Fetching wallet for user ID: {}", userId);

        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet", "userId", userId));

        return WalletResponse.from(wallet);
    }

    /**
     * Retrieves a wallet directly by its own ID.
     * Used internally and for admin lookups.
     */
    @Override
    @Transactional(readOnly = true)
    public WalletResponse getWalletById(Long walletId) {
        log.debug("Fetching wallet with ID: {}", walletId);

        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet", "id", walletId));

        return WalletResponse.from(wallet);
    }
}
