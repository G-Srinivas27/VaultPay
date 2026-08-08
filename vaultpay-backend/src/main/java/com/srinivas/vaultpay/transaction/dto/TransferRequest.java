package com.srinivas.vaultpay.transaction.dto;

import com.srinivas.vaultpay.common.validation.PositiveAmount;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * DTO for a peer-to-peer transfer request.
 *
 * <p>Note: the SOURCE wallet ID is a path variable in the controller URL,
 * not in this DTO. This DTO only carries the TARGET and amount.
 * Business rule enforcement (cannot send to self) happens in the service.
 *
 * @param toWalletId  the recipient's wallet ID
 * @param amount      the amount to transfer — must be present and greater than zero
 * @param description optional payment note (like UPI remarks)
 */
public record TransferRequest(

        @NotNull(message = "Recipient wallet ID is required")
        @Schema(description = "The RECIPIENT wallet's ID (not user ID)", example = "2")
        Long toWalletId,

        @NotNull(message = "Amount is required")
        @PositiveAmount
        @DecimalMax(value = "1000000.00", message = "Amount cannot exceed ₹10,00,000 per transaction")
        @Schema(description = "Amount to transfer — must be > 0, ≤ 10,00,000 and ≤ sender balance", example = "100.00")
        BigDecimal amount,

        @Schema(description = "Optional payment note (like UPI remarks)", example = "Rent for July")
        String description
) {}
