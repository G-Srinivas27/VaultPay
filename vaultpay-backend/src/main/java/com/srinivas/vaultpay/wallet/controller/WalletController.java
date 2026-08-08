package com.srinivas.vaultpay.wallet.controller;

import com.srinivas.vaultpay.common.response.ApiResponse;
import com.srinivas.vaultpay.wallet.dto.WalletResponse;
import com.srinivas.vaultpay.wallet.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/wallets")
@Tag(name = "Wallet Management", description = "Create wallets and check balances")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @PostMapping("/{userId}")
    @Operation(
            summary = "Create wallet for a user",
            description = "Creates a new INR wallet for the given user ID. Each user can only have one wallet."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Wallet created with zero balance"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "User already has a wallet")
    })
    public ResponseEntity<ApiResponse<WalletResponse>> createWallet(
            @Parameter(description = "The user's ID", example = "1")
            @PathVariable Long userId) {

        WalletResponse response = walletService.createWallet(userId);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Wallet created successfully", response));
    }

    @GetMapping("/user/{userId}")
    @Operation(
            summary = "Get wallet by user ID",
            description = "Returns the wallet belonging to the specified user."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Wallet retrieved"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No wallet found for this user")
    })
    public ResponseEntity<ApiResponse<WalletResponse>> getWalletByUserId(
            @Parameter(description = "The user's ID", example = "1")
            @PathVariable Long userId) {

        WalletResponse response = walletService.getWalletByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success("Wallet retrieved successfully", response));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get wallet by wallet ID",
            description = "Returns a wallet by its own ID (not the user ID)."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Wallet retrieved"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Wallet not found")
    })
    public ResponseEntity<ApiResponse<WalletResponse>> getWalletById(
            @Parameter(description = "The wallet's ID", example = "1")
            @PathVariable Long id) {

        WalletResponse response = walletService.getWalletById(id);
        return ResponseEntity.ok(ApiResponse.success("Wallet retrieved successfully", response));
    }
}
