package com.srinivas.vaultpay.transaction.dto;

import com.srinivas.vaultpay.common.validation.PositiveAmount;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * DTO for a money withdrawal request.
 *
 * @param amount      the amount to withdraw — must be present and greater than zero
 * @param description optional note about this withdrawal
 */
public record WithdrawRequest(

        @NotNull(message = "Amount is required")
        @PositiveAmount
        @DecimalMax(value = "1000000.00", message = "Amount cannot exceed ₹10,00,000 per transaction")
        @Schema(description = "Amount to withdraw — must be > 0, ≤ 10,00,000 and ≤ balance", example = "200.00")
        BigDecimal amount,

        @Schema(description = "Optional note about this withdrawal", example = "ATM withdrawal")
        String description
) {}
