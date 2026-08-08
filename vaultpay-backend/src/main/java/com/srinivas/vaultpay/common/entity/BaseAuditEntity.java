package com.srinivas.vaultpay.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Base class providing automatic audit fields for all JPA entities.
 *
 * <p><b>@MappedSuperclass:</b>
 * JPA will include ALL fields defined here in the child entity's database table.
 * This class itself is NOT an entity — it has no table of its own.
 * It purely contributes columns to whichever entity extends it.
 *
 * <p><b>@EntityListeners(AuditingEntityListener.class):</b>
 * Registers Spring Data's built-in listener. It automatically populates
 * the @CreatedDate, @LastModifiedDate, @CreatedBy, @LastModifiedBy fields
 * before each INSERT or UPDATE operation — no manual @PrePersist needed.
 * This annotation IS inherited by all subclasses (JPA spec guarantees this).
 *
 * <p><b>Why a base class instead of repeating fields?</b>
 * DRY principle (Don't Repeat Yourself). Without this, every entity would
 * need the same 4 fields + 2 lifecycle methods = 20+ lines of boilerplate.
 * With this base class: {@code class User extends BaseAuditEntity} — done.
 *
 * <p><b>Who populates @CreatedBy / @LastModifiedBy?</b>
 * Our {@link com.srinivas.vaultpay.config.AuditorAwareImpl} reads the
 * currently authenticated user's email from Spring's SecurityContextHolder.
 * For unauthenticated operations (e.g., user registration), it falls back to "SYSTEM".
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public abstract class BaseAuditEntity {

    /**
     * Timestamp when the record was first created.
     * Set automatically on INSERT, never changed after that.
     */
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp of the most recent update to the record.
     * Set automatically on INSERT and on every UPDATE.
     */
    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Email of the user who created this record.
     * Populated from SecurityContext at INSERT time.
     * "SYSTEM" for operations performed outside an authenticated context.
     */
    @CreatedBy
    @Column(updatable = false, length = 200)
    private String createdBy;

    /**
     * Email of the user who last modified this record.
     * Populated from SecurityContext at UPDATE time.
     */
    @LastModifiedBy
    @Column(length = 200)
    private String lastModifiedBy;
}
