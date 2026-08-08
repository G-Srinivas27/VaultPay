package com.srinivas.vaultpay.transaction.entity;

/**
 * Represents the type of a financial transaction.
 *
 * <p>Three fundamental operations in any digital wallet:
 * <ul>
 *   <li>CREDIT — money coming INTO a wallet (deposit or receiving a transfer)</li>
 *   <li>DEBIT  — money going OUT of a wallet (withdrawal or sending a transfer)</li>
 *   <li>TRANSFER — peer-to-peer movement between two VaultPay wallets.
 *       Internally generates a DEBIT record for the sender and a
 *       CREDIT record for the receiver.</li>
 * </ul>
 */
public enum TransactionType {
    CREDIT,    // Money received (deposit or inbound transfer)
    DEBIT,     // Money sent (withdrawal or outbound transfer)
    TRANSFER   // Peer-to-peer transfer (spawns DEBIT + CREDIT records)
}
