package com.srinivas.vaultpay.transaction.service;

import com.srinivas.vaultpay.transaction.dto.DepositRequest;
import com.srinivas.vaultpay.transaction.dto.TransactionResponse;
import com.srinivas.vaultpay.transaction.dto.TransferRequest;
import com.srinivas.vaultpay.transaction.dto.WithdrawRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Service interface for all financial transaction operations.
 */
public interface TransactionService {

    /**
     * Credits money into a wallet (deposit).
     *
     * @param walletId the wallet to deposit into
     * @param request  deposit amount and description
     * @return the created transaction record
     */
    TransactionResponse deposit(Long walletId, DepositRequest request);

    /**
     * Debits money from a wallet (withdrawal).
     *
     * <p>Business rules:
     * <ul>
     *   <li>Wallet must be ACTIVE</li>
     *   <li>Sufficient balance required — no overdraft</li>
     * </ul>
     *
     * @param walletId the wallet to withdraw from
     * @param request  withdrawal amount and description
     * @return the created transaction record
     */
    TransactionResponse withdraw(Long walletId, WithdrawRequest request);

    /**
     * Transfers money atomically between two wallets.
     *
     * <p>Business rules:
     * <ul>
     *   <li>Both wallets must be ACTIVE</li>
     *   <li>Source wallet must have sufficient balance</li>
     *   <li>Cannot transfer to the same wallet</li>
     * </ul>
     *
     * @param fromWalletId the sender's wallet ID
     * @param request      transfer details (toWalletId, amount, description)
     * @return the DEBIT transaction record for the sender
     */
    TransactionResponse transfer(Long fromWalletId, TransferRequest request);

    /**
     * Retrieves a page of transaction history for a wallet.
     *
     * @param walletId the wallet to get history for
     * @param pageable pagination and sorting parameters
     * @return page of transactions
     */
    Page<TransactionResponse> getTransactionHistory(Long walletId, Pageable pageable);
}
