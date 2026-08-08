package com.srinivas.vaultpay.transaction.dto;

import com.srinivas.vaultpay.transaction.entity.Transaction;
import com.srinivas.vaultpay.transaction.entity.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for transaction data returned in API responses.
 *
 * <p>Contains enough information for a user to understand what happened:
 * the type, amount, resulting balance, and who the counterparty was (if any).
 *
 * @param id                   unique transaction ID
 * @param walletId             the primary wallet for this record
 * @param counterpartyWalletId the other wallet involved (null for deposit/withdraw)
 * @param type                 CREDIT, DEBIT, or TRANSFER
 * @param amount               transaction amount (always positive)
 * @param balanceAfter         wallet balance immediately after this transaction
 * @param description          optional note
 * @param createdAt            when the transaction occurred
 */
public record TransactionResponse(
        Long id,
        Long walletId,
        Long counterpartyWalletId,
        TransactionType type,
        BigDecimal amount,
        BigDecimal balanceAfter,
        String description,
        LocalDateTime createdAt
) {

    /**
     * Factory method to map a Transaction entity to TransactionResponse.
     * Handles the nullable counterpartyWallet gracefully with a ternary.
     */
    public static TransactionResponse from(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getWallet().getId(),
                transaction.getCounterpartyWallet() != null
                        ? transaction.getCounterpartyWallet().getId()
                        : null,
                transaction.getType(),
                transaction.getAmount(),
                transaction.getBalanceAfter(),
                transaction.getDescription(),
                transaction.getCreatedAt()
        );
    }
}
