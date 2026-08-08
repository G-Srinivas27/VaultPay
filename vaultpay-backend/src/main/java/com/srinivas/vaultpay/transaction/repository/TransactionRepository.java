package com.srinivas.vaultpay.transaction.repository;

import com.srinivas.vaultpay.transaction.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Data access layer for Transaction entity.
 *
 * <p>This repository introduces our first custom {@code @Query} annotation.
 * Query derivation (method name parsing) works great for simple lookups,
 * but for complex conditions — like "transactions where wallet is EITHER
 * the primary OR the counterparty" — we write JPQL directly.
 *
 * <p><b>JPQL vs SQL:</b>
 * JPQL (Java Persistence Query Language) works on ENTITY names and field names,
 * not table/column names. Hibernate translates it to the correct SQL dialect
 * (H2 for dev, PostgreSQL for prod) automatically.
 * {@code Transaction t} → the Transaction entity class
 * {@code t.wallet.id} → navigates the @ManyToOne relationship
 */
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    /**
     * Retrieves a PAGE of transactions involving a specific wallet —
     * whether the wallet is the primary wallet OR the counterparty.
     *
     * <p><b>Why a separate countQuery?</b>
     * For pagination, Spring needs TWO queries:
     * 1. The data query (with LIMIT/OFFSET)
     * 2. A COUNT query (to compute totalPages and totalElements)
     *
     * When a @Query has ORDER BY or complex JOINs, Hibernate can't always
     * auto-derive the COUNT query correctly. Providing {@code countQuery}
     * explicitly avoids this. The count query is simpler (no ORDER BY)
     * so it runs faster.
     *
     * <p>The {@code Pageable} parameter handles LIMIT, OFFSET and sorting —
     * we let Pageable control the ORDER BY instead of hardcoding it in JPQL.
     *
     * @param walletId the wallet ID to fetch history for
     * @param pageable  pagination and sorting parameters
     * @return page of related transactions
     */
    @Query(value = "SELECT t FROM Transaction t " +
                   "WHERE t.wallet.id = :walletId OR t.counterpartyWallet.id = :walletId",
           countQuery = "SELECT COUNT(t) FROM Transaction t " +
                        "WHERE t.wallet.id = :walletId OR t.counterpartyWallet.id = :walletId")
    Page<Transaction> findAllByWalletId(@Param("walletId") Long walletId, Pageable pageable);
}
