package com.srinivas.vaultpay.transaction.entity;

import com.srinivas.vaultpay.wallet.entity.Wallet;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * JPA Entity representing a single financial transaction in VaultPay.
 *
 * <p><b>Ledger design — why store both wallet sides?</b>
 * A transfer involves two wallets: sender and receiver. Rather than one record,
 * we store the transaction with references to both wallets:
 * <ul>
 *   <li>{@code wallet} — the PRIMARY wallet for this record (always set)</li>
 *   <li>{@code counterpartyWallet} — the OTHER wallet involved (null for deposit/withdraw)</li>
 * </ul>
 *
 * <p>This gives us a complete audit trail:
 * "Given wallet ID 1, show me all transactions involving wallet 1"
 * → Query: WHERE wallet_id = 1 OR counterparty_wallet_id = 1
 *
 * <p><b>Why @ManyToOne for wallet?</b>
 * One wallet can have MANY transactions. This is a Many-to-One relationship:
 * many Transaction records → one Wallet.
 *
 * <p><b>Why is 'description' nullable?</b>
 * Deposits and withdrawals may not have a description. Transfers may include
 * an optional note (like UPI remarks). Nullable is appropriate here.
 *
 * <p><b>Why updatable=false on ALL fields except updatedAt?</b>
 * Transactions are immutable financial records. Once created, they must NEVER
 * be modified — this is a core accounting principle (audit trail integrity).
 * If a transaction needs to be reversed, a NEW compensating transaction is created.
 */
@Entity
@Table(name = "transactions")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The wallet this transaction record belongs to.
     * For CREDIT/DEBIT: this is the only wallet involved.
     * For TRANSFER: the sender's wallet (paired with a separate CREDIT record on receiver's side).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false, updatable = false)
    private Wallet wallet;

    /**
     * The other wallet involved in a transfer.
     * Null for standalone CREDIT (deposit) or DEBIT (withdrawal) transactions.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "counterparty_wallet_id", updatable = false)
    private Wallet counterpartyWallet;

    /**
     * The transaction amount. Always positive — the TransactionType indicates direction.
     * Using BigDecimal for exact financial arithmetic.
     */
    @Column(nullable = false, precision = 19, scale = 4, updatable = false)
    private BigDecimal amount;

    /**
     * The wallet balance AFTER this transaction was applied.
     * Storing the post-transaction balance makes statement generation trivial —
     * no need to recalculate by replaying history.
     */
    @Column(nullable = false, precision = 19, scale = 4, updatable = false)
    private BigDecimal balanceAfter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, updatable = false)
    private TransactionType type;

    /**
     * Optional note about the transaction (e.g., "Rent payment", "Dinner split").
     * Equivalent to a UPI transaction remarks field.
     */
    @Column(length = 255)
    private String description;

    /**
     * Immutable timestamp — set once on INSERT, never changed.
     * Transactions are financial records: they must NEVER be modified.
     * @CreatedDate replaces the manual @PrePersist we had before.
     */
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Who initiated this transaction — populated automatically from SecurityContext.
     * Useful for audit trails: "which user's session created this transaction record?"
     */
    @CreatedBy
    @Column(updatable = false, length = 200)
    private String createdBy;
}
