package com.srinivas.vaultpay.transaction.controller;

import com.srinivas.vaultpay.common.response.ApiResponse;
import com.srinivas.vaultpay.common.response.PagedResponse;
import com.srinivas.vaultpay.transaction.dto.DepositRequest;
import com.srinivas.vaultpay.transaction.dto.TransactionResponse;
import com.srinivas.vaultpay.transaction.dto.TransferRequest;
import com.srinivas.vaultpay.transaction.dto.WithdrawRequest;
import com.srinivas.vaultpay.transaction.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transactions")
@Tag(name = "Transactions", description = "Deposit, withdraw, transfer money and view transaction history")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/{walletId}/deposit")
    @Operation(
            summary = "Deposit money into wallet",
            description = "Credits the specified amount to the wallet. Creates a CREDIT transaction record."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Deposit successful"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid amount"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Wallet not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Wallet is FROZEN or CLOSED")
    })
    public ResponseEntity<ApiResponse<TransactionResponse>> deposit(
            @Parameter(description = "The wallet's ID", example = "1")
            @PathVariable Long walletId,
            @Valid @RequestBody DepositRequest request) {

        TransactionResponse response = transactionService.deposit(walletId, request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Deposit successful", response));
    }

    @PostMapping("/{walletId}/withdraw")
    @Operation(
            summary = "Withdraw money from wallet",
            description = "Debits the specified amount from the wallet. Fails if balance is insufficient or wallet is not ACTIVE."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Withdrawal successful"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid amount"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Wallet not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Insufficient balance or wallet not ACTIVE")
    })
    public ResponseEntity<ApiResponse<TransactionResponse>> withdraw(
            @Parameter(description = "The wallet's ID", example = "1")
            @PathVariable Long walletId,
            @Valid @RequestBody WithdrawRequest request) {

        TransactionResponse response = transactionService.withdraw(walletId, request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Withdrawal successful", response));
    }

    @PostMapping("/{walletId}/transfer")
    @Operation(
            summary = "Transfer money to another wallet",
            description = """
                    Atomically transfers money from one wallet to another.
                    Creates two transaction records (DEBIT on sender, CREDIT on receiver).
                    The entire operation is wrapped in a single database transaction —
                    if anything fails, both records are rolled back.
                    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Transfer successful"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid amount or same wallet transfer"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Sender or receiver wallet not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Insufficient balance or sender wallet not ACTIVE")
    })
    public ResponseEntity<ApiResponse<TransactionResponse>> transfer(
            @Parameter(description = "The SENDER wallet's ID", example = "1")
            @PathVariable Long walletId,
            @Valid @RequestBody TransferRequest request) {

        TransactionResponse response = transactionService.transfer(walletId, request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Transfer successful", response));
    }

    @GetMapping("/{walletId}/history")
    @Operation(
            summary = "Get transaction history (paginated)",
            description = "Returns all transactions involving this wallet (both sent and received). Supports ?page, ?size, ?sort."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "History retrieved"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Wallet not found")
    })
    public ResponseEntity<ApiResponse<PagedResponse<TransactionResponse>>> getHistory(
            @Parameter(description = "The wallet's ID", example = "1")
            @PathVariable Long walletId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        Page<TransactionResponse> page = transactionService.getTransactionHistory(walletId, pageable);
        return ResponseEntity.ok(
                ApiResponse.success("Transaction history retrieved", PagedResponse.from(page))
        );
    }
}
