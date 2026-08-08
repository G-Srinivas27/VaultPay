package com.srinivas.vaultpay.transaction.dto;

import com.srinivas.vaultpay.common.validation.PositiveAmount;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * DTO for a money deposit request.
 *
 * <p><b>Before:</b>
 * <pre>
 *   @NotNull(message = "Amount is required")
 *   @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
 *   BigDecimal amount;
 * </pre>
 *
 * <p><b>After (with custom annotation):</b>
 * <pre>
 *   @NotNull(message = "Amount is required")
 *   @PositiveAmount
 *   BigDecimal amount;
 * </pre>
 *
 * @NotNull     — "you must provide an amount"
 * @PositiveAmount — "the amount you provide must be > 0"
 * Two separate concerns, two separate annotations. Clean and readable.
 *
 * @param amount      the amount to deposit — must be present and greater than zero
 * @param description optional note about this deposit
 */
public record DepositRequest(

        @NotNull(message = "Amount is required")
        @PositiveAmount
        @DecimalMax(value = "1000000.00", message = "Amount cannot exceed ₹10,00,000 per transaction")
        @Schema(description = "Amount to deposit — must be > 0 and ≤ 10,00,000", example = "500.00")
        BigDecimal amount,

        @Schema(description = "Optional note about this deposit", example = "Monthly savings")
        String description
) {}
