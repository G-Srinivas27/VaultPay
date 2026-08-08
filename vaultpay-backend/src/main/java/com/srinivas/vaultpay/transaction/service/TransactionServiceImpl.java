package com.srinivas.vaultpay.transaction.service;

import com.srinivas.vaultpay.common.exception.BusinessException;
import com.srinivas.vaultpay.common.exception.ResourceNotFoundException;
import com.srinivas.vaultpay.transaction.dto.DepositRequest;
import com.srinivas.vaultpay.transaction.dto.TransactionResponse;
import com.srinivas.vaultpay.transaction.dto.TransferRequest;
import com.srinivas.vaultpay.transaction.dto.WithdrawRequest;
import com.srinivas.vaultpay.transaction.entity.Transaction;
import com.srinivas.vaultpay.transaction.entity.TransactionType;
import com.srinivas.vaultpay.transaction.repository.TransactionRepository;
import com.srinivas.vaultpay.email.service.EmailService;
import com.srinivas.vaultpay.wallet.entity.Wallet;
import com.srinivas.vaultpay.wallet.entity.WalletStatus;
import com.srinivas.vaultpay.wallet.repository.WalletRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Core financial operations implementation for VaultPay.
 *
 * <p><b>The most critical class in the system.</b>
 * Every method here touches money — correctness, atomicity, and
 * clear business rule enforcement are non-negotiable.
 *
 * <p><b>@Transactional on transfer() — the key design decision:</b>
 * The transfer method modifies TWO wallet balances and creates TWO transaction
 * records. All four of these DB writes happen inside ONE database transaction.
 * If ANY of them fail, ALL changes are rolled back automatically by Spring.
 * This guarantees money can never disappear or appear from nowhere.
 *
 * <p><b>BigDecimal arithmetic rules:</b>
 * <ul>
 *   <li>Always use {@code compareTo()} for comparison — NEVER use {@code equals()}.
 *       {@code new BigDecimal("1.0").equals(new BigDecimal("1.00"))} returns FALSE
 *       because equals() considers scale. compareTo() only compares value.</li>
 *   <li>Use {@code subtract()}, {@code add()} — never {@code -} or {@code +} operators</li>
 * </ul>
 */
@Service
public class TransactionServiceImpl implements TransactionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionServiceImpl.class);

    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;
    private final EmailService emailService;

    public TransactionServiceImpl(TransactionRepository transactionRepository,
                                  WalletRepository walletRepository,
                                  EmailService emailService) {
        this.transactionRepository = transactionRepository;
        this.walletRepository = walletRepository;
        this.emailService = emailService;
    }

    /**
     * Deposits money into a wallet.
     *
     * <p>Flow:
     * 1. Load and validate wallet (must exist and be ACTIVE)
     * 2. Add amount to balance
     * 3. Save updated wallet
     * 4. Record the transaction
     */
    @Override
    @Transactional
    public TransactionResponse deposit(Long walletId, DepositRequest request) {
        log.debug("Processing deposit of {} to wallet ID: {}", request.amount(), walletId);

        Wallet wallet = getActiveWallet(walletId);

        // BigDecimal.add() returns a NEW BigDecimal — BigDecimal is immutable
        BigDecimal newBalance = wallet.getBalance().add(request.amount());
        wallet.setBalance(newBalance);
        walletRepository.save(wallet);

        Transaction transaction = Transaction.builder()
                .wallet(wallet)
                .amount(request.amount())
                .balanceAfter(newBalance)
                .type(TransactionType.CREDIT)
                .description(request.description())
                .build();

        Transaction saved = transactionRepository.save(transaction);
        log.info("Deposit of {} successful. Wallet ID: {}, New balance: {}", request.amount(), walletId, newBalance);

        emailService.sendTransactionEmail(wallet.getUser(), "CREDIT", request.amount(), newBalance, saved.getCreatedAt());

        return TransactionResponse.from(saved);
    }

    /**
     * Withdraws money from a wallet.
     *
     * <p>Key business rule: balance cannot go below zero.
     * We use BigDecimal.compareTo() — not equals() or < operator.
     */
    @Override
    @Transactional
    public TransactionResponse withdraw(Long walletId, WithdrawRequest request) {
        log.debug("Processing withdrawal of {} from wallet ID: {}", request.amount(), walletId);

        Wallet wallet = getActiveWallet(walletId);

        // compareTo() returns: -1 (less than), 0 (equal), 1 (greater than)
        // wallet.balance < request.amount → insufficient funds
        if (wallet.getBalance().compareTo(request.amount()) < 0) {
            throw new BusinessException(
                    "Insufficient balance. Available: " + wallet.getBalance()
                    + ", Requested: " + request.amount()
            );
        }

        BigDecimal newBalance = wallet.getBalance().subtract(request.amount());
        wallet.setBalance(newBalance);
        walletRepository.save(wallet);

        Transaction transaction = Transaction.builder()
                .wallet(wallet)
                .amount(request.amount())
                .balanceAfter(newBalance)
                .type(TransactionType.DEBIT)
                .description(request.description())
                .build();

        Transaction saved = transactionRepository.save(transaction);
        log.info("Withdrawal of {} successful. Wallet ID: {}, New balance: {}", request.amount(), walletId, newBalance);

        emailService.sendTransactionEmail(wallet.getUser(), "DEBIT", request.amount(), newBalance, saved.getCreatedAt());

        return TransactionResponse.from(saved);
    }

    /**
     * Transfers money atomically between two wallets.
     *
     * <p><b>This is @Transactional — the CRITICAL guarantee:</b>
     * All operations below (2 balance updates + 2 transaction records) execute
     * within a single database transaction. If anything fails at any step,
     * the entire operation rolls back. No partial state is ever committed.
     *
     * <p>Flow:
     * 1. Validate: both wallets exist, both are ACTIVE, not same wallet
     * 2. Validate: sender has sufficient balance
     * 3. Deduct from sender, record DEBIT transaction
     * 4. Credit to receiver, record CREDIT transaction
     * 5. Return the sender's DEBIT record as confirmation
     */
    @Override
    @Transactional
    public TransactionResponse transfer(Long fromWalletId, TransferRequest request) {
        log.debug("Processing transfer of {} from wallet {} to wallet {}",
                request.amount(), fromWalletId, request.toWalletId());

        // Rule 1: Cannot transfer to yourself
        if (fromWalletId.equals(request.toWalletId())) {
            throw new BusinessException("Cannot transfer funds to the same wallet.");
        }

        Wallet senderWallet = getActiveWallet(fromWalletId);
        Wallet receiverWallet = getActiveWallet(request.toWalletId());

        // Rule 2: Sufficient balance
        if (senderWallet.getBalance().compareTo(request.amount()) < 0) {
            throw new BusinessException(
                    "Insufficient balance. Available: " + senderWallet.getBalance()
                    + ", Requested: " + request.amount()
            );
        }

        // Step 3: Deduct from sender
        BigDecimal senderNewBalance = senderWallet.getBalance().subtract(request.amount());
        senderWallet.setBalance(senderNewBalance);
        walletRepository.save(senderWallet);

        // Step 4: Credit to receiver
        BigDecimal receiverNewBalance = receiverWallet.getBalance().add(request.amount());
        receiverWallet.setBalance(receiverNewBalance);
        walletRepository.save(receiverWallet);

        // Record DEBIT for sender
        Transaction debitTx = Transaction.builder()
                .wallet(senderWallet)
                .counterpartyWallet(receiverWallet)
                .amount(request.amount())
                .balanceAfter(senderNewBalance)
                .type(TransactionType.TRANSFER)
                .description(request.description())
                .build();
        Transaction savedDebit = transactionRepository.save(debitTx);

        // Record CREDIT for receiver
        Transaction creditTx = Transaction.builder()
                .wallet(receiverWallet)
                .counterpartyWallet(senderWallet)
                .amount(request.amount())
                .balanceAfter(receiverNewBalance)
                .type(TransactionType.TRANSFER)
                .description(request.description())
                .build();
        transactionRepository.save(creditTx);

        log.info("Transfer of {} from wallet {} to wallet {} successful.",
                request.amount(), fromWalletId, request.toWalletId());

        emailService.sendTransactionEmail(senderWallet.getUser(), "DEBIT (Transfer Out)", request.amount(), senderNewBalance, savedDebit.getCreatedAt());
        emailService.sendTransactionEmail(receiverWallet.getUser(), "CREDIT (Transfer In)", request.amount(), receiverNewBalance, savedDebit.getCreatedAt());

        // Return the sender's debit record as confirmation to the caller
        return TransactionResponse.from(savedDebit);
    }

    /**
     * Returns a page of transaction history for a wallet.
     *
     * <p>Same Page.map() pattern as UserServiceImpl — maps entity Page to DTO Page
     * while preserving all pagination metadata.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<TransactionResponse> getTransactionHistory(Long walletId, Pageable pageable) {
        if (!walletRepository.existsById(walletId)) {
            throw new ResourceNotFoundException("Wallet", "id", walletId);
        }

        return transactionRepository.findAllByWalletId(walletId, pageable)
                .map(TransactionResponse::from);  // Page<Transaction> → Page<TransactionResponse>
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    /**
     * Loads a wallet and validates it is in ACTIVE status.
     * Extracted as a private helper to avoid duplicating the
     * "find + status check" logic across deposit, withdraw, and transfer.
     *
     * <p>This is the DRY principle (Don't Repeat Yourself) in action.
     */
    private Wallet getActiveWallet(Long walletId) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet", "id", walletId));

        if (wallet.getStatus() != WalletStatus.ACTIVE) {
            throw new BusinessException(
                    "Wallet ID " + walletId + " is not active. Current status: " + wallet.getStatus()
            );
        }
        return wallet;
    }
}
