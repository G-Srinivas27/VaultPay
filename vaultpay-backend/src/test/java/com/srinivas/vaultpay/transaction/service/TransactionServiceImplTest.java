package com.srinivas.vaultpay.transaction.service;

import com.srinivas.vaultpay.common.exception.BusinessException;
import com.srinivas.vaultpay.common.exception.ResourceNotFoundException;
import com.srinivas.vaultpay.transaction.dto.DepositRequest;
import com.srinivas.vaultpay.transaction.dto.TransactionResponse;
import com.srinivas.vaultpay.transaction.dto.TransferRequest;
import com.srinivas.vaultpay.transaction.dto.WithdrawRequest;
import com.srinivas.vaultpay.transaction.entity.Transaction;
import com.srinivas.vaultpay.transaction.entity.TransactionType;
import com.srinivas.vaultpay.email.service.EmailService;
import com.srinivas.vaultpay.transaction.repository.TransactionRepository;
import com.srinivas.vaultpay.user.entity.Role;
import com.srinivas.vaultpay.user.entity.User;
import com.srinivas.vaultpay.wallet.entity.Wallet;
import com.srinivas.vaultpay.wallet.entity.WalletStatus;
import com.srinivas.vaultpay.wallet.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

/**
 * Unit tests for {@link TransactionServiceImpl} — the most critical class in VaultPay.
 *
 * <p><b>New concept introduced: ArgumentCaptor</b>
 * Sometimes we need to verify not just THAT a method was called, but also
 * exactly WHAT arguments it was called with. For example, after a deposit,
 * we want to verify that the Transaction saved to the DB has the correct
 * amount, type, and balanceAfter.
 *
 * <p>ArgumentCaptor "captures" the argument passed to a mock method so we
 * can assert on it. Think of it like recording what goes INTO a method call.
 *
 * <pre>
 *   ArgumentCaptor&lt;Transaction&gt; captor = ArgumentCaptor.forClass(Transaction.class);
 *   then(transactionRepository).should().save(captor.capture()); // captures the saved object
 *   Transaction saved = captor.getValue();                       // get what was captured
 *   assertThat(saved.getAmount()).isEqualTo(new BigDecimal("500.00")); // assert on it
 * </pre>
 *
 * <p><b>BigDecimal comparison in tests:</b>
 * Use {@code isEqualByComparingTo()} from AssertJ — it uses compareTo() internally,
 * so "500.00" equals "500.0" equals "500". Never use .isEqualTo() for BigDecimal
 * as it also checks scale.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TransactionService Unit Tests")
class TransactionServiceImplTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    // ─── Shared test data ────────────────────────────────────────────────────

    private User owner;
    private Wallet activeWallet;
    private Wallet frozenWallet;
    private Wallet receiverWallet;

    /**
     * @BeforeEach — fresh test data before every test.
     * Wallets are built with a fixed balance so each test starts predictably.
     */
    @BeforeEach
    void setUp() {
        owner = User.builder()
                .id(1L)
                .firstName("Srinivas")
                .lastName("Gooda")
                .email("srinivas@vaultpay.com")
                .password("$2a$10$hashedPass")
                .role(Role.USER)
                .active(true)
                .build();

        // Active wallet with ₹1000 balance — used for most happy path tests
        activeWallet = Wallet.builder()
                .id(1L)
                .user(owner)
                .balance(new BigDecimal("1000.00"))
                .currency("INR")
                .status(WalletStatus.ACTIVE)
                .build();

        // Frozen wallet — used to test status validation
        frozenWallet = Wallet.builder()
                .id(2L)
                .user(owner)
                .balance(new BigDecimal("500.00"))
                .currency("INR")
                .status(WalletStatus.FROZEN)   // ← key difference
                .build();

        // Receiver wallet (different user) — used for transfer tests
        User receiver = User.builder()
                .id(2L)
                .firstName("Jane")
                .email("jane@vaultpay.com")
                .password("$2a$10$janeHash")
                .role(Role.USER)
                .active(true)
                .build();

        receiverWallet = Wallet.builder()
                .id(3L)
                .user(receiver)
                .balance(BigDecimal.ZERO)
                .currency("INR")
                .status(WalletStatus.ACTIVE)
                .build();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // DEPOSIT TESTS
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Deposit")
    class DepositTests {

        @Test
        @DisplayName("should deposit successfully and return correct response")
        void shouldDepositSuccessfully() {
            // GIVEN
            DepositRequest request = new DepositRequest(new BigDecimal("500.00"), "Salary");

            // Build the Transaction entity the repository will "return" after saving
            Transaction savedTransaction = Transaction.builder()
                    .id(1L)
                    .wallet(activeWallet)
                    .amount(new BigDecimal("500.00"))
                    .balanceAfter(new BigDecimal("1500.00"))
                    .type(TransactionType.CREDIT)
                    .description("Salary")
                    .createdAt(LocalDateTime.now())
                    .build();

            given(walletRepository.findById(1L)).willReturn(Optional.of(activeWallet));
            given(walletRepository.save(any(Wallet.class))).willReturn(activeWallet);
            given(transactionRepository.save(any(Transaction.class))).willReturn(savedTransaction);

            // WHEN
            TransactionResponse response = transactionService.deposit(1L, request);

            // THEN — verify response fields
            assertThat(response).isNotNull();
            assertThat(response.type()).isEqualTo(TransactionType.CREDIT);
            assertThat(response.amount()).isEqualByComparingTo("500.00");
            assertThat(response.balanceAfter()).isEqualByComparingTo("1500.00");
            assertThat(response.walletId()).isEqualTo(1L);

            // THEN — verify the wallet balance was actually updated before saving
            // ArgumentCaptor captures the Wallet passed to walletRepository.save()
            // so we can assert it had the correct new balance
            ArgumentCaptor<Wallet> walletCaptor = ArgumentCaptor.forClass(Wallet.class);
            then(walletRepository).should().save(walletCaptor.capture());
            assertThat(walletCaptor.getValue().getBalance())
                    .isEqualByComparingTo("1500.00");  // 1000 + 500

            // THEN — verify a Transaction record was saved
            then(transactionRepository).should().save(any(Transaction.class));
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when wallet does not exist")
        void shouldThrowWhenWalletNotFound() {
            // GIVEN
            given(walletRepository.findById(99L)).willReturn(Optional.empty());

            // WHEN + THEN
            assertThatThrownBy(() ->
                    transactionService.deposit(99L, new DepositRequest(new BigDecimal("500.00"), null))
            )
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("99");

            // THEN — wallet save and transaction save should NEVER be called
            then(walletRepository).should(never()).save(any());
            then(transactionRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("should throw BusinessException when wallet is FROZEN")
        void shouldThrowWhenWalletIsFrozen() {
            // GIVEN — frozenWallet has status = FROZEN
            given(walletRepository.findById(2L)).willReturn(Optional.of(frozenWallet));

            // WHEN + THEN
            assertThatThrownBy(() ->
                    transactionService.deposit(2L, new DepositRequest(new BigDecimal("100.00"), null))
            )
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("not active")
                    .hasMessageContaining("FROZEN");

            // No money movement should happen
            then(walletRepository).should(never()).save(any());
            then(transactionRepository).should(never()).save(any());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // WITHDRAW TESTS
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Withdraw")
    class WithdrawTests {

        @Test
        @DisplayName("should withdraw successfully when balance is sufficient")
        void shouldWithdrawSuccessfully() {
            // GIVEN — wallet has ₹1000, withdrawing ₹300
            WithdrawRequest request = new WithdrawRequest(new BigDecimal("300.00"), "ATM");

            Transaction savedTransaction = Transaction.builder()
                    .id(2L)
                    .wallet(activeWallet)
                    .amount(new BigDecimal("300.00"))
                    .balanceAfter(new BigDecimal("700.00"))
                    .type(TransactionType.DEBIT)
                    .description("ATM")
                    .createdAt(LocalDateTime.now())
                    .build();

            given(walletRepository.findById(1L)).willReturn(Optional.of(activeWallet));
            given(walletRepository.save(any(Wallet.class))).willReturn(activeWallet);
            given(transactionRepository.save(any(Transaction.class))).willReturn(savedTransaction);

            // WHEN
            TransactionResponse response = transactionService.withdraw(1L, request);

            // THEN
            assertThat(response.type()).isEqualTo(TransactionType.DEBIT);
            assertThat(response.amount()).isEqualByComparingTo("300.00");
            assertThat(response.balanceAfter()).isEqualByComparingTo("700.00");

            // Verify the wallet was saved with the deducted balance
            ArgumentCaptor<Wallet> walletCaptor = ArgumentCaptor.forClass(Wallet.class);
            then(walletRepository).should().save(walletCaptor.capture());
            assertThat(walletCaptor.getValue().getBalance())
                    .isEqualByComparingTo("700.00");  // 1000 - 300
        }

        @Test
        @DisplayName("should throw BusinessException when balance is insufficient")
        void shouldThrowWhenInsufficientBalance() {
            // GIVEN — wallet has ₹1000, trying to withdraw ₹9999
            WithdrawRequest request = new WithdrawRequest(new BigDecimal("9999.00"), "Large withdrawal");

            given(walletRepository.findById(1L)).willReturn(Optional.of(activeWallet));

            // WHEN + THEN
            assertThatThrownBy(() -> transactionService.withdraw(1L, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Insufficient balance")
                    .hasMessageContaining("1000")   // shows available balance
                    .hasMessageContaining("9999");  // shows requested amount

            // Nothing saved — short-circuit on balance check
            then(walletRepository).should(never()).save(any());
            then(transactionRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("should throw BusinessException when withdrawing exact balance amount (boundary)")
        void shouldSucceedWhenWithdrawingExactBalance() {
            // GIVEN — withdraw EXACTLY the available balance (boundary condition)
            // This tests the compareTo edge case: balance == amount → should SUCCEED
            WithdrawRequest request = new WithdrawRequest(new BigDecimal("1000.00"), "Full withdrawal");

            Transaction savedTransaction = Transaction.builder()
                    .id(3L)
                    .wallet(activeWallet)
                    .amount(new BigDecimal("1000.00"))
                    .balanceAfter(BigDecimal.ZERO)
                    .type(TransactionType.DEBIT)
                    .createdAt(LocalDateTime.now())
                    .build();

            given(walletRepository.findById(1L)).willReturn(Optional.of(activeWallet));
            given(walletRepository.save(any(Wallet.class))).willReturn(activeWallet);
            given(transactionRepository.save(any(Transaction.class))).willReturn(savedTransaction);

            // WHEN
            TransactionResponse response = transactionService.withdraw(1L, request);

            // THEN — zero balance is valid
            assertThat(response.balanceAfter()).isEqualByComparingTo("0");
        }

        @Test
        @DisplayName("should throw BusinessException when wallet is FROZEN")
        void shouldThrowWhenWalletIsFrozen() {
            // GIVEN
            given(walletRepository.findById(2L)).willReturn(Optional.of(frozenWallet));

            // WHEN + THEN
            assertThatThrownBy(() ->
                    transactionService.withdraw(2L, new WithdrawRequest(new BigDecimal("100.00"), null))
            )
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("not active");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // TRANSFER TESTS
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Transfer")
    class TransferTests {

        @Test
        @DisplayName("should transfer successfully and create two transaction records")
        void shouldTransferSuccessfully() {
            // GIVEN — wallet 1 has ₹1000, transferring ₹400 to wallet 3
            TransferRequest request = new TransferRequest(3L, new BigDecimal("400.00"), "Dinner split");

            // The DEBIT record returned for the sender
            Transaction debitTx = Transaction.builder()
                    .id(4L)
                    .wallet(activeWallet)
                    .counterpartyWallet(receiverWallet)
                    .amount(new BigDecimal("400.00"))
                    .balanceAfter(new BigDecimal("600.00"))
                    .type(TransactionType.TRANSFER)
                    .description("Dinner split")
                    .createdAt(LocalDateTime.now())
                    .build();

            given(walletRepository.findById(1L)).willReturn(Optional.of(activeWallet));
            given(walletRepository.findById(3L)).willReturn(Optional.of(receiverWallet));
            given(walletRepository.save(any(Wallet.class))).willReturn(activeWallet);
            // save() is called TWICE (sender + receiver wallets) and TWICE for transactions
            // We return different values on consecutive calls using thenReturn chaining
            given(transactionRepository.save(any(Transaction.class)))
                    .willReturn(debitTx)   // 1st call → sender's DEBIT record
                    .willReturn(Transaction.builder().id(5L).build()); // 2nd call → receiver's CREDIT

            // WHEN
            TransactionResponse response = transactionService.transfer(1L, request);

            // THEN — response is the sender's DEBIT record
            assertThat(response.type()).isEqualTo(TransactionType.TRANSFER);
            assertThat(response.amount()).isEqualByComparingTo("400.00");
            assertThat(response.balanceAfter()).isEqualByComparingTo("600.00");  // 1000 - 400
            assertThat(response.walletId()).isEqualTo(1L);
            assertThat(response.counterpartyWalletId()).isEqualTo(3L);

            // THEN — verify BOTH wallets were saved (two balance updates)
            then(walletRepository).should(times(2)).save(any(Wallet.class));

            // THEN — verify TWO transaction records were created (debit + credit)
            then(transactionRepository).should(times(2)).save(any(Transaction.class));
        }

        @Test
        @DisplayName("should throw BusinessException when transferring to the same wallet")
        void shouldThrowWhenSameWalletTransfer() {
            // GIVEN — fromWalletId == toWalletId
            TransferRequest request = new TransferRequest(1L, new BigDecimal("100.00"), "Self");

            // WHEN + THEN
            assertThatThrownBy(() -> transactionService.transfer(1L, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("same wallet");

            // Short-circuit — no DB calls should be made at all
            then(walletRepository).should(never()).findById(any());
            then(transactionRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when sender wallet does not exist")
        void shouldThrowWhenSenderWalletNotFound() {
            // GIVEN
            given(walletRepository.findById(99L)).willReturn(Optional.empty());
            TransferRequest request = new TransferRequest(3L, new BigDecimal("100.00"), null);

            // WHEN + THEN
            assertThatThrownBy(() -> transactionService.transfer(99L, request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("99");

            then(transactionRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when receiver wallet does not exist")
        void shouldThrowWhenReceiverWalletNotFound() {
            // GIVEN — sender exists, receiver does not
            given(walletRepository.findById(1L)).willReturn(Optional.of(activeWallet));
            given(walletRepository.findById(99L)).willReturn(Optional.empty());
            TransferRequest request = new TransferRequest(99L, new BigDecimal("100.00"), null);

            // WHEN + THEN
            assertThatThrownBy(() -> transactionService.transfer(1L, request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("99");

            then(transactionRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("should throw BusinessException when sender wallet is FROZEN")
        void shouldThrowWhenSenderWalletIsFrozen() {
            // GIVEN — sender is frozen, receiver is active
            given(walletRepository.findById(2L)).willReturn(Optional.of(frozenWallet));
            TransferRequest request = new TransferRequest(3L, new BigDecimal("100.00"), null);

            // WHEN + THEN
            assertThatThrownBy(() -> transactionService.transfer(2L, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("not active")
                    .hasMessageContaining("FROZEN");

            then(transactionRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("should throw BusinessException when sender has insufficient balance")
        void shouldThrowWhenSenderHasInsufficientBalance() {
            // GIVEN — sender has ₹1000, trying to send ₹5000
            given(walletRepository.findById(1L)).willReturn(Optional.of(activeWallet));
            given(walletRepository.findById(3L)).willReturn(Optional.of(receiverWallet));
            TransferRequest request = new TransferRequest(3L, new BigDecimal("5000.00"), null);

            // WHEN + THEN
            assertThatThrownBy(() -> transactionService.transfer(1L, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Insufficient balance")
                    .hasMessageContaining("1000")
                    .hasMessageContaining("5000");

            // No wallets should be saved, no transactions created
            then(walletRepository).should(never()).save(any());
            then(transactionRepository).should(never()).save(any());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // TRANSACTION HISTORY TESTS
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Get Transaction History")
    class GetTransactionHistoryTests {

        @Test
        @DisplayName("should return paginated transaction history for a valid wallet")
        void shouldReturnTransactionHistory() {
            // GIVEN
            Pageable pageable = PageRequest.of(0, 10);

            Transaction tx1 = Transaction.builder()
                    .id(1L).wallet(activeWallet)
                    .amount(new BigDecimal("500.00"))
                    .balanceAfter(new BigDecimal("1500.00"))
                    .type(TransactionType.CREDIT)
                    .createdAt(LocalDateTime.now())
                    .build();

            Transaction tx2 = Transaction.builder()
                    .id(2L).wallet(activeWallet)
                    .amount(new BigDecimal("200.00"))
                    .balanceAfter(new BigDecimal("1300.00"))
                    .type(TransactionType.DEBIT)
                    .createdAt(LocalDateTime.now())
                    .build();

            Page<Transaction> txPage = new PageImpl<>(List.of(tx1, tx2), pageable, 2);

            given(walletRepository.existsById(1L)).willReturn(true);
            given(transactionRepository.findAllByWalletId(1L, pageable)).willReturn(txPage);

            // WHEN
            Page<TransactionResponse> result = transactionService.getTransactionHistory(1L, pageable);

            // THEN
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getTotalElements()).isEqualTo(2);
            assertThat(result.getContent().get(0).type()).isEqualTo(TransactionType.CREDIT);
            assertThat(result.getContent().get(1).type()).isEqualTo(TransactionType.DEBIT);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when wallet does not exist")
        void shouldThrowWhenWalletNotFound() {
            // GIVEN
            given(walletRepository.existsById(99L)).willReturn(false);

            // WHEN + THEN
            assertThatThrownBy(() ->
                    transactionService.getTransactionHistory(99L, PageRequest.of(0, 10))
            )
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("99");

            // Repository should never be queried for transactions
            then(transactionRepository).should(never()).findAllByWalletId(any(), any());
        }
    }
}
