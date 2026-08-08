package com.srinivas.vaultpay.wallet.entity;

import com.srinivas.vaultpay.common.entity.BaseAuditEntity;
import com.srinivas.vaultpay.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * JPA Entity representing a VaultPay digital wallet.
 *
 * <p><b>@OneToOne relationship explained:</b>
 * Each wallet belongs to exactly one user. We store the foreign key
 * (user_id column) in the WALLET table, not in the USERS table.
 * This is the "owning side" of the relationship — the side that holds
 * the foreign key column is always the owning side.
 *
 * <p><b>Why unidirectional?</b>
 * The {@code User} entity has NO reference back to {@code Wallet}.
 * This keeps the User module independent of the Wallet module.
 * If we later extract Wallet into a separate microservice, User needs
 * zero changes. This is the "loose coupling" principle in practice.
 *
 * <p><b>Why BigDecimal for balance?</b>
 * Financial calculations require EXACT decimal arithmetic.
 * double/float use binary floating point — 0.1 + 0.2 = 0.30000000000000004.
 * BigDecimal is arbitrary-precision and exact. ALWAYS use it for money.
 *
 * <p><b>Column precision(19, 4):</b>
 * <ul>
 *   <li>precision=19 — up to 19 total digits</li>
 *   <li>scale=4 — up to 4 decimal places (e.g., 1234567890.1234)</li>
 * </ul>
 * This supports balances up to ~1 quadrillion with 4 decimal places — enough
 * for any currency including crypto.
 *
 * <p><b>Audit fields:</b>
 * createdAt, updatedAt, createdBy, lastModifiedBy are inherited from
 * {@link BaseAuditEntity} and auto-populated by Spring Data JPA Auditing.
 */
@Entity
@Table(name = "wallets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Wallet extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The owner of this wallet.
     *
     * <p>@OneToOne — one wallet belongs to one user.
     * @JoinColumn — creates a 'user_id' foreign key column in the 'wallets' table.
     * nullable=false — every wallet MUST have an owner.
     * unique=true — enforces the one-wallet-per-user constraint at DB level.
     * updatable=false — once set, the owner can never be changed (immutable ownership).
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true, updatable = false)
    private User user;

    /**
     * The wallet balance.
     *
     * <p>@Column(precision=19, scale=4) maps BigDecimal to DECIMAL(19,4) in the DB.
     * Without this, Hibernate may use a default that doesn't match your precision needs.
     * Always be explicit about precision for financial columns.
     *
     * <p>Initialized to ZERO — a new wallet starts with no funds.
     * Never initialize to null, that would require null-checks everywhere.
     */
    @Column(nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    /**
     * ISO 4217 currency code (e.g., "INR", "USD", "EUR").
     * Defaulting to INR for VaultPay's initial market.
     * Future: support multi-currency wallets.
     */
    @Column(nullable = false, length = 3)
    @Builder.Default
    private String currency = "INR";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private WalletStatus status = WalletStatus.ACTIVE;
}
