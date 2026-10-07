package com.revana.bank.account.service;

import com.revana.bank.account.dto.*;
import com.revana.bank.account.entity.Account;
import com.revana.bank.account.exception.AccessDeniedException;
import com.revana.bank.account.exception.AccountNotFoundException;
import com.revana.bank.account.kafka.AuditProducer;
import com.revana.bank.account.repository.AccountRepository;
import com.revana.bank.account.repository.TransactionRepository;
import com.revana.bank.account.client.AuthClient;
import com.revana.bank.account.client.NotificationClient;
import com.revana.bank.account.config.RateLimiterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AccountService.
 *
 * Strategy:
 *  - All external dependencies (repositories, Feign clients, Kafka producers,
 *    internal service wrappers) are replaced with Mockito mocks so that the
 *    test suite runs without a database, Kafka broker, or any live service.
 *  - @ExtendWith(MockitoExtension.class) wires mocks into @InjectMocks
 *    automatically through constructor injection — matching AccountService's
 *    all-args constructor.
 *  - Every public method that belongs to the "tested six" (createAccount,
 *    getAccount, deposit, withdraw, transfer, closeAccount) has a Success,
 *    a Failure, and a Validation test.
 *
 * File location:
 *   src/test/java/com/revana/bank/account/service/AccountServiceTest.java
 */
@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    // -----------------------------------------------------------------------
    // Mocks — one per constructor parameter of AccountService
    // -----------------------------------------------------------------------

    @Mock
    private AccountRepository repository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransactionClientService transactionService;

    @Mock
    private NotificationClient notificationClient;

    @Mock
    private NotificationService notificationService;

    @Mock
    private KafkaProducerService kafkaProducer;

    @Mock
    private TransactionEventProducer eventProducer;

    @Mock
    private NotificationEventProducer notificationProducer;

    @Mock
    private AuthClient authClient;

    @Mock
    private RateLimiterService rateLimiterService;

    @Mock
    private AuditProducer auditProducer;

    /**
     * System-under-test.
     * Mockito's @InjectMocks constructs AccountService via its
     * all-args constructor, injecting every @Mock field above.
     */
    @InjectMocks
    private AccountService accountService;

    // -----------------------------------------------------------------------
    // Shared test fixtures
    // -----------------------------------------------------------------------

    /** A fully-populated Account used as the "happy path" return value. */
    private Account sampleAccount;

    @BeforeEach
    void setUp() {
        sampleAccount = Account.builder()
                .id(1L)
                .userId(100L)
                .customerId("CUST-001")
                .accountNumber("ACC-XYZ123")
                .customerName("John Doe")
                .accountType("SAVINGS")
                .balance(new BigDecimal("5000.00"))
                .status("ACTIVE")
                .createdAt(LocalDateTime.now())
                .build();
    }

    // =======================================================================
    // 1.  createAccount()
    // =======================================================================

    @Nested
    @DisplayName("createAccount()")
    class CreateAccountTests {

        /**
         * SUCCESS — createAccount()
         *
         * What is being tested:
         *   Given a valid CreateAccountRequest for a customer that does NOT yet
         *   have an account, createAccount() must:
         *     1. Persist a new Account via repository.save().
         *     2. Publish an ACCOUNT_CREATED audit event via auditProducer.
         *     3. Return the saved Account.
         *
         * Mocks used:
         *   - repository.existsByCustomerId()  → stubbed to return false (no duplicate)
         *   - repository.save()                → stubbed to return sampleAccount
         *   - auditProducer.publish()          → verified it was called once
         *
         * Why when(): pre-program the repository stubs so no real DB call is made.
         * Why verify(): assert that audit event production is a side effect
         *               the service MUST perform on every successful account creation.
         */
        @Test
        @DisplayName("should save account and publish audit event on success")
        void createAccount_success() {
            // Arrange
            CreateAccountRequest request = new CreateAccountRequest();
            request.setUserId(100L);
            request.setCustomerId("CUST-001");
            request.setCustomerName("John Doe");
            request.setAccountType("SAVINGS");
            request.setBalance(new BigDecimal("5000.00"));

            when(repository.existsByCustomerId("CUST-001")).thenReturn(false);
            when(repository.save(any(Account.class))).thenReturn(sampleAccount);

            // Act
            Account result = accountService.createAccount(request);

            // Assert — returned value
            assertThat(result).isNotNull();
            assertThat(result.getCustomerId()).isEqualTo("CUST-001");
            assertThat(result.getCustomerName()).isEqualTo("John Doe");

            // Assert — repository was called to persist
            verify(repository, times(1)).save(any(Account.class));

            // Assert — audit event was published (side-effect verification)
            ArgumentCaptor<AuditEvent> auditCaptor = ArgumentCaptor.forClass(AuditEvent.class);
            verify(auditProducer, times(1)).publish(auditCaptor.capture());
            assertThat(auditCaptor.getValue().getEventType()).isEqualTo("ACCOUNT_CREATED");
            assertThat(auditCaptor.getValue().getActionStatus()).isEqualTo("SUCCESS");
        }

        /**
         * FAILURE — createAccount() with duplicate customer
         *
         * What is being tested:
         *   When a customer already owns an account, createAccount() must throw
         *   RuntimeException with message "Account already exists for customer"
         *   and must NOT attempt to save or publish anything.
         *
         * Mocks used:
         *   - repository.existsByCustomerId() → stubbed to return true
         *
         * Why when(): simulate the duplicate-detection branch.
         * Why verify(): confirm no save() and no audit event are triggered
         *               when the guard throws.
         */
        @Test
        @DisplayName("should throw RuntimeException when account already exists for customer")
        void createAccount_duplicate_throwsRuntimeException() {
            // Arrange
            CreateAccountRequest request = new CreateAccountRequest();
            request.setCustomerId("CUST-001");

            when(repository.existsByCustomerId("CUST-001")).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> accountService.createAccount(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Account already exists for customer");

            // Verify no persistence or audit happened
            verify(repository, never()).save(any());
            verify(auditProducer, never()).publish(any());
        }

        /**
         * VALIDATION — createAccount() with null customerId
         *
         * What is being tested:
         *   existsByCustomerId(null) returns false (no duplicate), but the
         *   Account is still built and persisted. This validates that the
         *   service does not add its own null-check on customerId — the
         *   constraint lives at the DB/validation layer, not here.
         *   The test confirms the service reaches repository.save() normally.
         *
         * Mocks used:
         *   - repository.existsByCustomerId(null) → false
         *   - repository.save()                   → sampleAccount
         */
        @Test
        @DisplayName("should still attempt save when customerId is null (DB-layer validation)")
        void createAccount_nullCustomerId_delegatesToRepository() {
            // Arrange
            CreateAccountRequest request = new CreateAccountRequest();
            request.setUserId(100L);
            request.setCustomerId(null);
            request.setCustomerName("Jane");
            request.setAccountType("CURRENT");
            request.setBalance(BigDecimal.ZERO);

            when(repository.existsByCustomerId(null)).thenReturn(false);
            when(repository.save(any(Account.class))).thenReturn(sampleAccount);

            // Act — should not throw; service trusts the DB constraint
            Account result = accountService.createAccount(request);

            // Assert
            assertThat(result).isNotNull();
            verify(repository, times(1)).save(any(Account.class));
        }
    }

    // =======================================================================
    // 2.  getAccount() — maps to "getAccountById" in the spec
    // =======================================================================

    @Nested
    @DisplayName("getAccount()")
    class GetAccountTests {

        /**
         * SUCCESS — getAccount()
         *
         * What is being tested:
         *   When the repository finds an account with the given id, getAccount()
         *   must return it without hitting the DB a second time.
         *
         * Mocks used:
         *   - repository.findById(1L) → Optional.of(sampleAccount)
         *
         * Why when(): supply the DB record so no real DB call is made.
         * Why verify(): confirm exactly one repository hit — no extra calls.
         */
        @Test
        @DisplayName("should return account when found by id")
        void getAccount_success() {
            // Arrange
            when(repository.findById(1L)).thenReturn(Optional.of(sampleAccount));

            // Act
            Account result = accountService.getAccount(1L);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getAccountNumber()).isEqualTo("ACC-XYZ123");

            verify(repository, times(1)).findById(1L);
        }

        /**
         * FAILURE — getAccount() when account does not exist
         *
         * What is being tested:
         *   When repository.findById() returns empty, getAccount() must throw
         *   AccountNotFoundException with message "Account Not Found".
         *
         * Mocks used:
         *   - repository.findById(99L) → Optional.empty()
         *
         * Why when(): simulate the missing-record case.
         * Why verify(): confirm a single repository call was made and nothing else.
         */
        @Test
        @DisplayName("should throw AccountNotFoundException when account not found")
        void getAccount_notFound_throwsAccountNotFoundException() {
            // Arrange
            when(repository.findById(99L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> accountService.getAccount(99L))
                    .isInstanceOf(AccountNotFoundException.class)
                    .hasMessage("Account Not Found");

            verify(repository, times(1)).findById(99L);
        }

        /**
         * VALIDATION — getAccount() with null id
         *
         * What is being tested:
         *   Passing null as id must result in AccountNotFoundException because
         *   repository.findById(null) returns empty (Mockito default).
         *   This confirms the service has no special null guard — the orElseThrow
         *   path handles it.
         */
        @Test
        @DisplayName("should throw AccountNotFoundException for null id")
        void getAccount_nullId_throwsAccountNotFoundException() {
            // Arrange — Mockito returns Optional.empty() by default for unstubbed calls
            when(repository.findById(null)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> accountService.getAccount(null))
                    .isInstanceOf(AccountNotFoundException.class)
                    .hasMessage("Account Not Found");
        }
    }

    // =======================================================================
    // 3.  deposit()
    // =======================================================================

    @Nested
    @DisplayName("deposit()")
    class DepositTests {

        /**
         * SUCCESS — deposit()
         *
         * What is being tested:
         *   A valid deposit by a CUSTOMER who owns the account must:
         *     1. Increase account balance by the deposited amount.
         *     2. Persist the updated account.
         *     3. Record a DEPOSIT transaction via transactionService.
         *     4. Send a deposit notification via notificationService.
         *     5. Publish an ACCOUNT_DEPOSIT audit event via auditProducer.
         *
         * Mocks used:
         *   - repository.findById()          → returns sampleAccount
         *   - repository.save()              → returns updated account
         *   - transactionService.createTransaction() → void, just verified
         *   - notificationService.sendNotification() → void, just verified
         *   - auditProducer.publish()                → void, captured & verified
         *
         * Why when(): pre-program find and save so no DB is needed.
         * Why verify(): every side effect (transaction record, notification, audit)
         *               is a contractual obligation of deposit(); missing any one
         *               is a bug.
         */
        @Test
        @DisplayName("should increase balance, record transaction, notify and audit on success")
        void deposit_success() {
            // Arrange
            BigDecimal depositAmount = new BigDecimal("1000.00");
            BigDecimal expectedBalance = new BigDecimal("6000.00");

            Account updatedAccount = Account.builder()
                    .id(1L)
                    .userId(100L)
                    .customerId("CUST-001")
                    .accountNumber("ACC-XYZ123")
                    .customerName("John Doe")
                    .accountType("SAVINGS")
                    .balance(expectedBalance)
                    .status("ACTIVE")
                    .createdAt(LocalDateTime.now())
                    .build();

            when(repository.findById(1L)).thenReturn(Optional.of(sampleAccount));
            when(repository.save(any(Account.class))).thenReturn(updatedAccount);

            // Act
            Account result = accountService.deposit(1L, depositAmount, 100L, "CUSTOMER");

            // Assert — returned balance reflects the deposit
            assertThat(result.getBalance()).isEqualByComparingTo(expectedBalance);

            // Verify persistence
            verify(repository, times(1)).save(any(Account.class));

            // Verify transaction was recorded
            ArgumentCaptor<TransactionRequest> txCaptor = ArgumentCaptor.forClass(TransactionRequest.class);
            verify(transactionService, times(1)).createTransaction(txCaptor.capture());
            assertThat(txCaptor.getValue().getTransactionType()).isEqualTo("DEPOSIT");
            assertThat(txCaptor.getValue().getAmount()).isEqualByComparingTo(depositAmount);

            // Verify notification was sent
            ArgumentCaptor<NotificationRequest> notifCaptor = ArgumentCaptor.forClass(NotificationRequest.class);
            verify(notificationService, times(1)).sendNotification(notifCaptor.capture());
            assertThat(notifCaptor.getValue().getAccountId()).isEqualTo(1L);

            // Verify audit event was published
            ArgumentCaptor<AuditEvent> auditCaptor = ArgumentCaptor.forClass(AuditEvent.class);
            verify(auditProducer, times(1)).publish(auditCaptor.capture());
            assertThat(auditCaptor.getValue().getEventType()).isEqualTo("ACCOUNT_DEPOSIT");
        }

        /**
         * FAILURE — deposit() when account is not found
         *
         * What is being tested:
         *   When no account exists for the given id, deposit() must throw
         *   AccountNotFoundException and not perform any side effects.
         *
         * Mocks used:
         *   - repository.findById(99L) → Optional.empty()
         *
         * Why verify(never()): confirms no balance mutation, no transaction,
         *   no notification, and no audit event are produced on failure.
         */
        @Test
        @DisplayName("should throw AccountNotFoundException when account not found")
        void deposit_accountNotFound_throwsAccountNotFoundException() {
            // Arrange
            when(repository.findById(99L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() ->
                    accountService.deposit(99L, new BigDecimal("500.00"), 100L, "CUSTOMER"))
                    .isInstanceOf(AccountNotFoundException.class)
                    .hasMessage("Account Not Found");

            // No side effects should occur
            verify(repository, never()).save(any());
            verify(transactionService, never()).createTransaction(any());
            verify(notificationService, never()).sendNotification(any());
            verify(auditProducer, never()).publish(any());
        }

        /**
         * VALIDATION — deposit() with zero amount
         *
         * What is being tested:
         *   A deposit of ZERO must be rejected immediately with
         *   RuntimeException("Deposit amount must be greater than zero").
         *   The repository must never be queried because the guard fires first.
         *
         * Why verify(never()): the guard is a pre-condition check;
         *   if even one repository call happens the service logic is wrong.
         */
        @Test
        @DisplayName("should throw RuntimeException for zero deposit amount")
        void deposit_zeroAmount_throwsRuntimeException() {
            // Act & Assert
            assertThatThrownBy(() ->
                    accountService.deposit(1L, BigDecimal.ZERO, 100L, "CUSTOMER"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Deposit amount must be greater than zero");

            // Guard must fire before any repository access
            verify(repository, never()).findById(anyLong());
        }

        /**
         * VALIDATION — deposit() with negative amount
         *
         * What is being tested:
         *   A negative deposit amount (e.g. -100) must be rejected with the
         *   same guard message as a zero amount.
         */
        @Test
        @DisplayName("should throw RuntimeException for negative deposit amount")
        void deposit_negativeAmount_throwsRuntimeException() {
            // Act & Assert
            assertThatThrownBy(() ->
                    accountService.deposit(1L, new BigDecimal("-100.00"), 100L, "CUSTOMER"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Deposit amount must be greater than zero");

            verify(repository, never()).findById(anyLong());
        }

        /**
         * VALIDATION — deposit() access denied for wrong user
         *
         * What is being tested:
         *   When the caller has role CUSTOMER but their userId does NOT match
         *   the account's userId, deposit() must throw AccessDeniedException.
         *
         * Mocks used:
         *   - repository.findById() → sampleAccount (userId = 100L)
         *   Caller supplies userId = 999L → mismatch.
         *
         * Why when(): need the account loaded before the ownership check runs.
         * Why verify(never()): no save/transaction/notification should occur on
         *   an access-denied path.
         */
        @Test
        @DisplayName("should throw AccessDeniedException when CUSTOMER accesses another user's account")
        void deposit_accessDenied_throwsAccessDeniedException() {
            // Arrange — account belongs to userId 100, but caller claims userId 999
            when(repository.findById(1L)).thenReturn(Optional.of(sampleAccount));

            // Act & Assert
            assertThatThrownBy(() ->
                    accountService.deposit(1L, new BigDecimal("500.00"), 999L, "CUSTOMER"))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("not allowed");

            verify(repository, never()).save(any());
            verify(auditProducer, never()).publish(any());
        }
    }

    // =======================================================================
    // 4.  withdraw()
    // =======================================================================

    @Nested
    @DisplayName("withdraw()")
    class WithdrawTests {

        /**
         * SUCCESS — withdraw()
         *
         * What is being tested:
         *   Withdrawing an amount that is less than or equal to the current balance
         *   must:
         *     1. Decrease the account balance correctly.
         *     2. Persist the account.
         *     3. Record a WITHDRAW transaction.
         *     4. Send a withdrawal notification.
         *     5. Publish an ACCOUNT_WITHDRAW audit event.
         *
         * Mocks used:
         *   - repository.findById()  → sampleAccount (balance 5000)
         *   - repository.save()      → account with reduced balance
         *
         * Why ArgumentCaptor for TransactionRequest:
         *   We want to assert the exact transactionType and amount that the
         *   service passes to transactionService — not just that it was called.
         */
        @Test
        @DisplayName("should decrease balance, record transaction, notify and audit on success")
        void withdraw_success() {
            // Arrange
            BigDecimal withdrawAmount = new BigDecimal("1000.00");
            BigDecimal expectedBalance = new BigDecimal("4000.00");

            Account updatedAccount = Account.builder()
                    .id(1L)
                    .userId(100L)
                    .customerId("CUST-001")
                    .accountNumber("ACC-XYZ123")
                    .customerName("John Doe")
                    .balance(expectedBalance)
                    .status("ACTIVE")
                    .createdAt(LocalDateTime.now())
                    .build();

            when(repository.findById(1L)).thenReturn(Optional.of(sampleAccount));
            when(repository.save(any(Account.class))).thenReturn(updatedAccount);

            // Act
            Account result = accountService.withdraw(1L, withdrawAmount);

            // Assert — balance reflects the withdrawal
            assertThat(result.getBalance()).isEqualByComparingTo(expectedBalance);

            // Verify persistence
            verify(repository, times(1)).save(any(Account.class));

            // Verify transaction record
            ArgumentCaptor<TransactionRequest> txCaptor = ArgumentCaptor.forClass(TransactionRequest.class);
            verify(transactionService, times(1)).createTransaction(txCaptor.capture());
            assertThat(txCaptor.getValue().getTransactionType()).isEqualTo("WITHDRAW");
            assertThat(txCaptor.getValue().getAmount()).isEqualByComparingTo(withdrawAmount);

            // Verify notification
            verify(notificationService, times(1)).sendNotification(any(NotificationRequest.class));

            // Verify audit
            ArgumentCaptor<AuditEvent> auditCaptor = ArgumentCaptor.forClass(AuditEvent.class);
            verify(auditProducer, times(1)).publish(auditCaptor.capture());
            assertThat(auditCaptor.getValue().getEventType()).isEqualTo("ACCOUNT_WITHDRAW");
        }

        /**
         * FAILURE — withdraw() when account is not found
         *
         * What is being tested:
         *   Missing account must raise AccountNotFoundException.
         *   No side effects (save, transaction, notification, audit) should occur.
         */
        @Test
        @DisplayName("should throw AccountNotFoundException when account not found")
        void withdraw_accountNotFound_throwsAccountNotFoundException() {
            // Arrange
            when(repository.findById(99L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> accountService.withdraw(99L, new BigDecimal("500.00")))
                    .isInstanceOf(AccountNotFoundException.class)
                    .hasMessage("Account Not Found");

            verify(repository, never()).save(any());
            verify(transactionService, never()).createTransaction(any());
            verify(notificationService, never()).sendNotification(any());
            verify(auditProducer, never()).publish(any());
        }

        /**
         * FAILURE — withdraw() with insufficient balance
         *
         * What is being tested:
         *   When the withdrawal amount exceeds current balance, the service must
         *   throw RuntimeException("Insufficient Balance") and roll back without
         *   persisting anything.
         *
         * Mocks used:
         *   - repository.findById() → sampleAccount with balance 5000
         *   Attempt to withdraw 9999 → exceeds balance.
         *
         * Why verify(never()): confirms the guard prevents any mutation when
         *   balance is insufficient.
         */
        @Test
        @DisplayName("should throw RuntimeException for insufficient balance")
        void withdraw_insufficientBalance_throwsRuntimeException() {
            // Arrange — account has 5000 but we try to withdraw 9999
            when(repository.findById(1L)).thenReturn(Optional.of(sampleAccount));

            // Act & Assert
            assertThatThrownBy(() -> accountService.withdraw(1L, new BigDecimal("9999.00")))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Insufficient Balance");

            verify(repository, never()).save(any());
            verify(auditProducer, never()).publish(any());
        }

        /**
         * VALIDATION — withdraw() with zero amount
         *
         * What is being tested:
         *   A zero-amount withdrawal must be rejected immediately before any
         *   repository lookup occurs.
         */
        @Test
        @DisplayName("should throw RuntimeException for zero withdrawal amount")
        void withdraw_zeroAmount_throwsRuntimeException() {
            // Act & Assert
            assertThatThrownBy(() -> accountService.withdraw(1L, BigDecimal.ZERO))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Withdrawal amount must be greater than zero");

            verify(repository, never()).findById(anyLong());
        }

        /**
         * VALIDATION — withdraw() with null amount
         *
         * What is being tested:
         *   A null amount must be caught by the null-check guard and produce
         *   the same validation exception before any repository access.
         */
        @Test
        @DisplayName("should throw RuntimeException for null withdrawal amount")
        void withdraw_nullAmount_throwsRuntimeException() {
            // Act & Assert
            assertThatThrownBy(() -> accountService.withdraw(1L, null))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Withdrawal amount must be greater than zero");

            verify(repository, never()).findById(anyLong());
        }
    }

    // =======================================================================
    // 5.  transfer()
    // =======================================================================

    @Nested
    @DisplayName("transfer()")
    class TransferTests {

        /**
         * SUCCESS — transfer()
         *
         * What is being tested:
         *   A valid transfer between two distinct accounts with sufficient
         *   sender balance must:
         *     1. Deduct from sender and credit receiver.
         *     2. Save both accounts.
         *     3. Create a TRANSFER transaction.
         *     4. Send notifications to BOTH sender and receiver.
         *     5. Publish an ACCOUNT_TRANSFER audit event.
         *     6. Attempt to publish a Kafka transfer event (fire-and-forget).
         *     7. Return a populated TransferResponse.
         *
         * Mocks used:
         *   - repository.findById(1L) → sender (balance 5000)
         *   - repository.findById(2L) → receiver (balance 2000)
         *   - repository.save()       → returns the saved account (called twice)
         *
         * Why ArgumentCaptor for NotificationRequest:
         *   Ensures the service sends TWO notifications — one to sender, one to
         *   receiver — with the correct account ids and messages.
         */
        @Test
        @DisplayName("should debit sender, credit receiver, notify both and return TransferResponse")
        void transfer_success() {
            // Arrange
            Account receiver = Account.builder()
                    .id(2L)
                    .userId(200L)
                    .customerId("CUST-002")
                    .accountNumber("ACC-ABC456")
                    .customerName("Jane Smith")
                    .balance(new BigDecimal("2000.00"))
                    .status("ACTIVE")
                    .createdAt(LocalDateTime.now())
                    .build();

            TransferRequest request = new TransferRequest();
            request.setFromAccountId(1L);
            request.setToAccountId(2L);
            request.setAmount(new BigDecimal("1000.00"));

            // Sender lookup then receiver lookup (called in order with different ids)
            when(repository.findById(1L)).thenReturn(Optional.of(sampleAccount));
            when(repository.findById(2L)).thenReturn(Optional.of(receiver));
            when(repository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

            // Act
            TransferResponse response = accountService.transfer(request);

            // Assert — TransferResponse fields
            assertThat(response).isNotNull();
            assertThat(response.getMessage()).isEqualTo("Transfer Successful");
            assertThat(response.getSenderName()).isEqualTo("John Doe");
            assertThat(response.getReceiverName()).isEqualTo("Jane Smith");
            assertThat(response.getTransferAmount()).isEqualByComparingTo(new BigDecimal("1000.00"));
            // Sender balance should have been reduced by 1000
            assertThat(response.getSenderBalance()).isEqualByComparingTo(new BigDecimal("4000.00"));
            // Receiver balance should have been increased by 1000
            assertThat(response.getReceiverBalance()).isEqualByComparingTo(new BigDecimal("3000.00"));

            // Verify both accounts were persisted
            verify(repository, times(2)).save(any(Account.class));

            // Verify one TRANSFER transaction was recorded
            ArgumentCaptor<TransactionRequest> txCaptor = ArgumentCaptor.forClass(TransactionRequest.class);
            verify(transactionService, times(1)).createTransaction(txCaptor.capture());
            assertThat(txCaptor.getValue().getTransactionType()).isEqualTo("TRANSFER");

            // Verify two notifications — one per party
            verify(notificationService, times(2)).sendNotification(any(NotificationRequest.class));

            // Verify audit event
            ArgumentCaptor<AuditEvent> auditCaptor = ArgumentCaptor.forClass(AuditEvent.class);
            verify(auditProducer, times(1)).publish(auditCaptor.capture());
            assertThat(auditCaptor.getValue().getEventType()).isEqualTo("ACCOUNT_TRANSFER");
        }

        /**
         * FAILURE — transfer() when sender account is not found
         *
         * What is being tested:
         *   When the sender's account id does not exist, transfer() must throw
         *   AccountNotFoundException("Sender Account Not Found") immediately and
         *   must not modify the receiver's account or produce any side effects.
         *
         * Mocks used:
         *   - repository.findById(99L) → Optional.empty()
         */
        @Test
        @DisplayName("should throw AccountNotFoundException when sender account not found")
        void transfer_senderNotFound_throwsAccountNotFoundException() {
            // Arrange
            TransferRequest request = new TransferRequest();
            request.setFromAccountId(99L);
            request.setToAccountId(2L);
            request.setAmount(new BigDecimal("500.00"));

            when(repository.findById(99L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> accountService.transfer(request))
                    .isInstanceOf(AccountNotFoundException.class)
                    .hasMessage("Sender Account Not Found");

            verify(repository, never()).save(any());
            verify(auditProducer, never()).publish(any());
            verify(transactionService, never()).createTransaction(any());
        }

        /**
         * FAILURE — transfer() when receiver account is not found
         *
         * What is being tested:
         *   When the sender exists but the receiver does not, transfer() must
         *   throw AccountNotFoundException("Receiver Account Not Found").
         *
         * Mocks used:
         *   - repository.findById(1L)  → sender exists
         *   - repository.findById(99L) → Optional.empty() (receiver missing)
         */
        @Test
        @DisplayName("should throw AccountNotFoundException when receiver account not found")
        void transfer_receiverNotFound_throwsAccountNotFoundException() {
            // Arrange
            TransferRequest request = new TransferRequest();
            request.setFromAccountId(1L);
            request.setToAccountId(99L);
            request.setAmount(new BigDecimal("500.00"));

            when(repository.findById(1L)).thenReturn(Optional.of(sampleAccount));
            when(repository.findById(99L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> accountService.transfer(request))
                    .isInstanceOf(AccountNotFoundException.class)
                    .hasMessage("Receiver Account Not Found");

            verify(repository, never()).save(any());
            verify(auditProducer, never()).publish(any());
        }

        /**
         * FAILURE — transfer() with insufficient sender balance
         *
         * What is being tested:
         *   When the sender's balance is less than the requested transfer amount,
         *   the service must throw RuntimeException("Insufficient Balance") and
         *   leave both accounts unchanged.
         *
         * Mocks used:
         *   - repository.findById(1L) → sampleAccount balance 5000
         *   - repository.findById(2L) → receiver
         *   Transfer amount 9999 > 5000 → should fail.
         */
        @Test
        @DisplayName("should throw RuntimeException for insufficient sender balance")
        void transfer_insufficientBalance_throwsRuntimeException() {
            // Arrange
            Account receiver = Account.builder()
                    .id(2L)
                    .userId(200L)
                    .accountNumber("ACC-ABC456")
                    .customerName("Jane Smith")
                    .balance(new BigDecimal("2000.00"))
                    .status("ACTIVE")
                    .createdAt(LocalDateTime.now())
                    .build();

            TransferRequest request = new TransferRequest();
            request.setFromAccountId(1L);
            request.setToAccountId(2L);
            request.setAmount(new BigDecimal("9999.00"));

            when(repository.findById(1L)).thenReturn(Optional.of(sampleAccount));
            when(repository.findById(2L)).thenReturn(Optional.of(receiver));

            // Act & Assert
            assertThatThrownBy(() -> accountService.transfer(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Insufficient Balance");

            verify(repository, never()).save(any());
            verify(auditProducer, never()).publish(any());
        }

        /**
         * VALIDATION — transfer() with zero amount
         *
         * What is being tested:
         *   A zero transfer amount must be rejected by the guard before any
         *   account lookup happens.
         *
         * Why verify(never()): proves the early-exit guard is effective.
         */
        @Test
        @DisplayName("should throw RuntimeException for zero transfer amount")
        void transfer_zeroAmount_throwsRuntimeException() {
            // Arrange
            TransferRequest request = new TransferRequest();
            request.setFromAccountId(1L);
            request.setToAccountId(2L);
            request.setAmount(BigDecimal.ZERO);

            // Act & Assert
            assertThatThrownBy(() -> accountService.transfer(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Transfer amount must be greater than zero");

            verify(repository, never()).findById(anyLong());
        }

        /**
         * VALIDATION — transfer() with null amount
         *
         * What is being tested:
         *   A null transfer amount must be caught by the null-guard and produce
         *   the same exception as a zero amount before any repository access.
         */
        @Test
        @DisplayName("should throw RuntimeException for null transfer amount")
        void transfer_nullAmount_throwsRuntimeException() {
            // Arrange
            TransferRequest request = new TransferRequest();
            request.setFromAccountId(1L);
            request.setToAccountId(2L);
            request.setAmount(null);

            // Act & Assert
            assertThatThrownBy(() -> accountService.transfer(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Transfer amount must be greater than zero");

            verify(repository, never()).findById(anyLong());
        }
    }

    // =======================================================================
    // 6.  closeAccount()
    // =======================================================================

    @Nested
    @DisplayName("closeAccount()")
    class CloseAccountTests {

        /**
         * SUCCESS — closeAccount()
         *
         * What is being tested:
         *   closeAccount() must:
         *     1. Find the account by id.
         *     2. Set status to "CLOSED" and populate closedAt timestamp.
         *     3. Persist the updated account.
         *     4. Publish an ACCOUNT_CLOSED audit event.
         *     5. Return the saved (closed) account.
         *
         * Mocks used:
         *   - repository.findById(1L)  → sampleAccount (status ACTIVE)
         *   - repository.save()        → the account passed to it (closed state)
         *
         * Why ArgumentCaptor for Account on save():
         *   We need to confirm the account passed to save() actually has
         *   status="CLOSED" set — not just that save() was called.
         *
         * Why ArgumentCaptor for AuditEvent:
         *   Confirms the eventType is exactly "ACCOUNT_CLOSED".
         */
        @Test
        @DisplayName("should set status to CLOSED, persist and publish audit event")
        void closeAccount_success() {
            // Arrange
            when(repository.findById(1L)).thenReturn(Optional.of(sampleAccount));
            when(repository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

            // Act
            Account result = accountService.closeAccount(1L);

            // Assert — status was changed before save
            assertThat(result.getStatus()).isEqualTo("CLOSED");
            assertThat(result.getClosedAt()).isNotNull();

            // Verify exactly one save call
            ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
            verify(repository, times(1)).save(accountCaptor.capture());
            assertThat(accountCaptor.getValue().getStatus()).isEqualTo("CLOSED");

            // Verify audit event
            ArgumentCaptor<AuditEvent> auditCaptor = ArgumentCaptor.forClass(AuditEvent.class);
            verify(auditProducer, times(1)).publish(auditCaptor.capture());
            assertThat(auditCaptor.getValue().getEventType()).isEqualTo("ACCOUNT_CLOSED");
            assertThat(auditCaptor.getValue().getActionStatus()).isEqualTo("SUCCESS");
        }

        /**
         * FAILURE — closeAccount() when account is not found
         *
         * What is being tested:
         *   When no account exists for the given id, closeAccount() must throw
         *   AccountNotFoundException and must not call save() or publish any
         *   audit event.
         *
         * Mocks used:
         *   - repository.findById(99L) → Optional.empty()
         */
        @Test
        @DisplayName("should throw AccountNotFoundException when account not found")
        void closeAccount_notFound_throwsAccountNotFoundException() {
            // Arrange
            when(repository.findById(99L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> accountService.closeAccount(99L))
                    .isInstanceOf(AccountNotFoundException.class)
                    .hasMessage("Account Not Found");

            verify(repository, never()).save(any());
            verify(auditProducer, never()).publish(any());
        }

        /**
         * VALIDATION — closeAccount() sets closedAt to a non-null timestamp
         *
         * What is being tested:
         *   The service must stamp closedAt on the account before persisting it.
         *   An account that has been closed without a timestamp would be a data
         *   integrity bug.
         *
         * Mocks used:
         *   - repository.findById(1L) → sampleAccount (closedAt == null)
         *   - repository.save()       → returns passed argument
         */
        @Test
        @DisplayName("should set closedAt timestamp when account is closed")
        void closeAccount_setsClosedAtTimestamp() {
            // Arrange — sampleAccount has no closedAt
            assertThat(sampleAccount.getClosedAt()).isNull();

            when(repository.findById(1L)).thenReturn(Optional.of(sampleAccount));
            when(repository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

            // Act
            Account result = accountService.closeAccount(1L);

            // Assert
            assertThat(result.getClosedAt())
                    .as("closedAt must be set during closeAccount()")
                    .isNotNull()
                    .isBeforeOrEqualTo(LocalDateTime.now());
        }
    }

    // =======================================================================
    // 7.  Additional edge-case / integration-style unit tests
    // =======================================================================

    @Nested
    @DisplayName("deposit() — ADMIN role bypass")
    class DepositAdminRoleTests {

        /**
         * SUCCESS — ADMIN can deposit into any account
         *
         * What is being tested:
         *   When the caller has role "ADMIN", the userId ownership check is
         *   skipped. An ADMIN with a completely different userId (999L) must
         *   still be able to deposit into any account without AccessDeniedException.
         *
         * Mocks used:
         *   - repository.findById(1L) → sampleAccount (userId = 100L)
         *   - repository.save()       → updated account
         */
        @Test
        @DisplayName("ADMIN role should bypass ownership check and deposit successfully")
        void deposit_adminRole_bypassesOwnershipCheck() {
            // Arrange — ADMIN whose userId does NOT match the account owner
            BigDecimal depositAmount = new BigDecimal("500.00");

            when(repository.findById(1L)).thenReturn(Optional.of(sampleAccount));
            when(repository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

            // Act — should NOT throw even though userId 999 != account.userId 100
            assertThatCode(() ->
                    accountService.deposit(1L, depositAmount, 999L, "ADMIN"))
                    .doesNotThrowAnyException();

            verify(repository, times(1)).save(any(Account.class));
        }
    }

    @Nested
    @DisplayName("transfer() — audit payload content")
    class TransferAuditTests {

        /**
         * Verifies the audit event payload contains the transfer amount and
         * the receiver account number — a security / traceability requirement.
         *
         * What is being tested:
         *   The AuditEvent published during transfer must have:
         *     - eventType  = "ACCOUNT_TRANSFER"
         *     - entityId   = sender's account number
         *     - payload    containing the transfer amount and receiver account number
         *
         * Mocks used:
         *   - repository.findById(1L) → sender
         *   - repository.findById(2L) → receiver
         *   - repository.save()       → passthrough
         */
        @Test
        @DisplayName("should publish audit event with correct transfer payload")
        void transfer_publishesAuditEventWithCorrectPayload() {
            // Arrange
            Account receiver = Account.builder()
                    .id(2L)
                    .userId(200L)
                    .accountNumber("ACC-RECV789")
                    .customerName("Jane Smith")
                    .balance(new BigDecimal("2000.00"))
                    .status("ACTIVE")
                    .createdAt(LocalDateTime.now())
                    .build();

            TransferRequest request = new TransferRequest();
            request.setFromAccountId(1L);
            request.setToAccountId(2L);
            request.setAmount(new BigDecimal("250.00"));

            when(repository.findById(1L)).thenReturn(Optional.of(sampleAccount));
            when(repository.findById(2L)).thenReturn(Optional.of(receiver));
            when(repository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

            // Act
            accountService.transfer(request);

            // Assert audit payload
            ArgumentCaptor<AuditEvent> auditCaptor = ArgumentCaptor.forClass(AuditEvent.class);
            verify(auditProducer, times(1)).publish(auditCaptor.capture());

            AuditEvent audit = auditCaptor.getValue();
            assertThat(audit.getEventType()).isEqualTo("ACCOUNT_TRANSFER");
            assertThat(audit.getEntityId()).isEqualTo("ACC-XYZ123"); // sender's account number
            assertThat(audit.getPayload()).contains("250.00");
            assertThat(audit.getPayload()).contains("ACC-RECV789"); // receiver account number
        }
    }
}
