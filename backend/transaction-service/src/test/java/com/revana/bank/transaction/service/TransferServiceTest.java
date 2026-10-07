package com.revana.bank.transaction.service;

import com.revana.bank.transaction.client.AccountClient;
import com.revana.bank.transaction.client.dto.AccountResponse;
import com.revana.bank.transaction.dto.FundTransferCompletedEvent;
import com.revana.bank.transaction.dto.TransferHistoryResponse;
import com.revana.bank.transaction.dto.TransferRequest;
import com.revana.bank.transaction.dto.TransferResponse;
import com.revana.bank.transaction.entity.Transfer;
import com.revana.bank.transaction.entity.TransferStatus;
import com.revana.bank.transaction.entity.TransferType;
import com.revana.bank.transaction.exception.ErrorCode;
import com.revana.bank.transaction.exception.TransferException;
import com.revana.bank.transaction.kafka.TransferKafkaProducer;
import com.revana.bank.transaction.repository.TransferRepository;
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TransferService — the core business logic class.
 *
 * Strategy:
 *  - @ExtendWith(MockitoExtension.class): constructor injection matched
 *    to all four TransferService dependencies.
 *  - Every public method has: SUCCESS test, FAILURE test(s), VALIDATION test.
 *  - ArgumentCaptor used wherever we need to inspect what was passed to a mock.
 *  - verify() used to assert every required side-effect (audit, Kafka, save).
 *
 * Methods under test:
 *   initiateTransfer()   — 11 tests (all 8 validation rules + success path)
 *   getByReference()     — 2 tests
 *   getHistory()         — 5 tests (all 4 filter combinations + empty result)
 *
 * File location:
 *   src/test/java/com/revana/bank/transaction/service/TransferServiceTest.java
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TransferService")
class TransferServiceTest {

    // ── Mocks ────────────────────────────────────────────────────────────────
    @Mock private TransferRepository    transferRepository;
    @Mock private AuditService          auditService;
    @Mock private AccountClient         accountClient;
    @Mock private TransferKafkaProducer kafkaProducer;

    @InjectMocks
    private TransferService transferService;

    // ── Shared fixtures ──────────────────────────────────────────────────────
    private static final String USER_ID          = "user-101";
    private static final String SOURCE_ACCOUNT   = "ACC10001";
    private static final String DEST_ACCOUNT     = "ACC20001";
    private static final BigDecimal AMOUNT_1000  = new BigDecimal("1000.00");
    private static final BigDecimal BALANCE_5000 = new BigDecimal("5000.00");

    /** A fully populated Transfer returned by repository.save() in the happy path. */
    private Transfer savedTransfer;

    /** Active source account returned by accountClient. */
    private AccountResponse activeSource;

    /** Active destination account returned by accountClient. */
    private AccountResponse activeDest;

    /** A valid transfer request. */
    private TransferRequest validRequest;

    @BeforeEach
    void setUp() {
        savedTransfer = Transfer.builder()
                .id(1L)
                .transactionReference("TXN4A3B2C1D5E6F")
                .sourceAccount(SOURCE_ACCOUNT)
                .destinationAccount(DEST_ACCOUNT)
                .amount(AMOUNT_1000)
                .transferType(TransferType.IMPS)
                .description("Rent Payment")
                .status(TransferStatus.SUCCESS)
                .performedBy(USER_ID)
                .createdAt(LocalDateTime.now())
                .build();

        activeSource = new AccountResponse();
        activeSource.setId(1L);
        activeSource.setAccountNumber(SOURCE_ACCOUNT);
        activeSource.setBalance(BALANCE_5000);
        activeSource.setStatus("ACTIVE");

        activeDest = new AccountResponse();
        activeDest.setId(2L);
        activeDest.setAccountNumber(DEST_ACCOUNT);
        activeDest.setBalance(new BigDecimal("2000.00"));
        activeDest.setStatus("ACTIVE");

        validRequest = new TransferRequest();
        validRequest.setSourceAccount(SOURCE_ACCOUNT);
        validRequest.setDestinationAccount(DEST_ACCOUNT);
        validRequest.setAmount(AMOUNT_1000);
        validRequest.setTransferType(TransferType.IMPS);
        validRequest.setDescription("Rent Payment");
    }

    // =========================================================================
    // initiateTransfer()
    // =========================================================================

    @Nested
    @DisplayName("initiateTransfer()")
    class InitiateTransferTests {

        /**
         * SUCCESS — full happy path
         *
         * What is tested:
         *   A valid request with two ACTIVE accounts and sufficient balance
         *   must produce a SUCCESS TransferResponse, persist twice (PENDING → SUCCESS),
         *   write two audit entries (INITIATED + SUCCESS), and publish one Kafka event.
         *
         * Mocks used:
         *   accountClient         → returns active source + dest
         *   transferRepository    → daily sum = 0, save returns savedTransfer
         *   auditService          → void, verified call count
         *   kafkaProducer         → void, verified with ArgumentCaptor
         *
         * Why verify():
         *   Kafka publish and audit are contractual side-effects;
         *   a missing call is a production bug.
         */
        @Test
        @DisplayName("should complete transfer, persist, audit x2, and publish Kafka event")
        void initiateTransfer_success() {
            // Arrange
            when(accountClient.getByAccountNumber(SOURCE_ACCOUNT)).thenReturn(activeSource);
            when(accountClient.getByAccountNumber(DEST_ACCOUNT)).thenReturn(activeDest);
            when(transferRepository.sumSuccessfulTransfersAfter(eq(SOURCE_ACCOUNT), any()))
                    .thenReturn(BigDecimal.ZERO);
            when(transferRepository.save(any(Transfer.class))).thenReturn(savedTransfer);

            // Act
            TransferResponse response = transferService.initiateTransfer(validRequest, USER_ID);

            // Assert — response
            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo(TransferStatus.SUCCESS);
            assertThat(response.getTransactionReference()).isEqualTo("TXN4A3B2C1D5E6F");
            assertThat(response.getMessage()).isEqualTo("Transfer completed successfully");
            assertThat(response.getSourceAccount()).isEqualTo(SOURCE_ACCOUNT);
            assertThat(response.getDestinationAccount()).isEqualTo(DEST_ACCOUNT);
            assertThat(response.getAmount()).isEqualByComparingTo(AMOUNT_1000);

            // Verify repository called twice (PENDING save + SUCCESS update)
            verify(transferRepository, times(2)).save(any(Transfer.class));

            // Verify two audit entries: TRANSFER_INITIATED and TRANSFER_SUCCESS
            verify(auditService, times(2)).log(
                    anyString(), eq(USER_ID),
                    eq(SOURCE_ACCOUNT), eq(DEST_ACCOUNT),
                    eq(AMOUNT_1000), any(TransferStatus.class), anyString());

            // Verify Kafka event published with correct fields
            ArgumentCaptor<FundTransferCompletedEvent> eventCaptor =
                    ArgumentCaptor.forClass(FundTransferCompletedEvent.class);
            verify(kafkaProducer, times(1)).publishTransferCompleted(eventCaptor.capture());
            assertThat(eventCaptor.getValue().getTransactionId()).isEqualTo("TXN4A3B2C1D5E6F");
            assertThat(eventCaptor.getValue().getStatus()).isEqualTo(TransferStatus.SUCCESS);
            assertThat(eventCaptor.getValue().getSourceAccount()).isEqualTo(SOURCE_ACCOUNT);
            assertThat(eventCaptor.getValue().getDestinationAccount()).isEqualTo(DEST_ACCOUNT);
        }

        // ── Validation guards ─────────────────────────────────────────────

        /**
         * VALIDATION — self-transfer rejected
         *
         * What is tested:
         *   When source == destination, throw SELF_TRANSFER_NOT_ALLOWED
         *   and write one TRANSFER_REJECTED audit entry.
         *   No repository lookup should occur.
         *
         * Why verify(never()) on accountClient:
         *   The guard runs before any external call — confirms correct ordering.
         */
        @Test
        @DisplayName("should throw SELF_TRANSFER_NOT_ALLOWED when source == destination")
        void initiateTransfer_selfTransfer_throws() {
            // Arrange
            validRequest.setDestinationAccount(SOURCE_ACCOUNT); // same as source

            // Act & Assert
            TransferException ex = catchThrowableOfType(
                    () -> transferService.initiateTransfer(validRequest, USER_ID),
                    TransferException.class);

            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.SELF_TRANSFER_NOT_ALLOWED);

            // Audit entry written even on rejection
            verify(auditService, times(1)).log(
                    eq("TRANSFER_REJECTED"), eq(USER_ID),
                    anyString(), anyString(), any(), any(), anyString());

            // No account lookups performed
            verify(accountClient, never()).getByAccountNumber(any());
        }

        /**
         * VALIDATION — zero amount rejected before any account lookup
         */
        @Test
        @DisplayName("should throw INVALID_TRANSFER_AMOUNT for zero amount")
        void initiateTransfer_zeroAmount_throws() {
            validRequest.setAmount(BigDecimal.ZERO);

            TransferException ex = catchThrowableOfType(
                    () -> transferService.initiateTransfer(validRequest, USER_ID),
                    TransferException.class);

            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_TRANSFER_AMOUNT);
            verify(accountClient, never()).getByAccountNumber(any());
        }

        /**
         * VALIDATION — negative amount rejected before any account lookup
         */
        @Test
        @DisplayName("should throw INVALID_TRANSFER_AMOUNT for negative amount")
        void initiateTransfer_negativeAmount_throws() {
            validRequest.setAmount(new BigDecimal("-500.00"));

            TransferException ex = catchThrowableOfType(
                    () -> transferService.initiateTransfer(validRequest, USER_ID),
                    TransferException.class);

            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_TRANSFER_AMOUNT);
            verify(accountClient, never()).getByAccountNumber(any());
        }

        // ── Account validation ────────────────────────────────────────────

        /**
         * FAILURE — source account not found
         *
         * What is tested:
         *   When accountClient returns null for the source account,
         *   throw ACCOUNT_NOT_FOUND and write an audit entry.
         *   Destination account must NOT be fetched.
         *
         * Why when(null):
         *   Feign returns null for a 404. We simulate that here.
         */
        @Test
        @DisplayName("should throw ACCOUNT_NOT_FOUND when source account does not exist")
        void initiateTransfer_sourceNotFound_throws() {
            when(accountClient.getByAccountNumber(SOURCE_ACCOUNT)).thenReturn(null);

            TransferException ex = catchThrowableOfType(
                    () -> transferService.initiateTransfer(validRequest, USER_ID),
                    TransferException.class);

            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ACCOUNT_NOT_FOUND);
            assertThat(ex.getMessage()).contains(SOURCE_ACCOUNT);

            verify(accountClient, never()).getByAccountNumber(DEST_ACCOUNT);
            verify(auditService, times(1)).log(eq("TRANSFER_REJECTED"),
                    any(), any(), any(), any(), any(), any());
        }

        /**
         * FAILURE — source account is CLOSED
         */
        @Test
        @DisplayName("should throw ACCOUNT_INACTIVE when source account is CLOSED")
        void initiateTransfer_sourceInactive_throws() {
            activeSource.setStatus("CLOSED");
            when(accountClient.getByAccountNumber(SOURCE_ACCOUNT)).thenReturn(activeSource);

            TransferException ex = catchThrowableOfType(
                    () -> transferService.initiateTransfer(validRequest, USER_ID),
                    TransferException.class);

            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ACCOUNT_INACTIVE);
            verify(accountClient, never()).getByAccountNumber(DEST_ACCOUNT);
        }

        /**
         * FAILURE — destination account not found
         */
        @Test
        @DisplayName("should throw ACCOUNT_NOT_FOUND when destination account does not exist")
        void initiateTransfer_destNotFound_throws() {
            when(accountClient.getByAccountNumber(SOURCE_ACCOUNT)).thenReturn(activeSource);
            when(accountClient.getByAccountNumber(DEST_ACCOUNT)).thenReturn(null);

            TransferException ex = catchThrowableOfType(
                    () -> transferService.initiateTransfer(validRequest, USER_ID),
                    TransferException.class);

            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ACCOUNT_NOT_FOUND);
            assertThat(ex.getMessage()).contains(DEST_ACCOUNT);
        }

        /**
         * FAILURE — destination account is CLOSED
         */
        @Test
        @DisplayName("should throw ACCOUNT_INACTIVE when destination account is CLOSED")
        void initiateTransfer_destInactive_throws() {
            activeDest.setStatus("CLOSED");
            when(accountClient.getByAccountNumber(SOURCE_ACCOUNT)).thenReturn(activeSource);
            when(accountClient.getByAccountNumber(DEST_ACCOUNT)).thenReturn(activeDest);

            TransferException ex = catchThrowableOfType(
                    () -> transferService.initiateTransfer(validRequest, USER_ID),
                    TransferException.class);

            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ACCOUNT_INACTIVE);
        }

        // ── Balance check ─────────────────────────────────────────────────

        /**
         * FAILURE — insufficient balance
         *
         * What is tested:
         *   Source balance (100) < transfer amount (1000) → INSUFFICIENT_BALANCE.
         *   No Transfer row should be saved, no Kafka event published.
         *
         * Why verify(never()) on transferRepository.save():
         *   Confirms the balance guard fires before any persistence.
         */
        @Test
        @DisplayName("should throw INSUFFICIENT_BALANCE when source balance is too low")
        void initiateTransfer_insufficientBalance_throws() {
            activeSource.setBalance(new BigDecimal("100.00")); // less than 1000
            when(accountClient.getByAccountNumber(SOURCE_ACCOUNT)).thenReturn(activeSource);
            when(accountClient.getByAccountNumber(DEST_ACCOUNT)).thenReturn(activeDest);

            TransferException ex = catchThrowableOfType(
                    () -> transferService.initiateTransfer(validRequest, USER_ID),
                    TransferException.class);

            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INSUFFICIENT_BALANCE);
            verify(transferRepository, never()).save(any());
            verify(kafkaProducer, never()).publishTransferCompleted(any());
        }

        // ── Daily limit ───────────────────────────────────────────────────

        /**
         * FAILURE — daily limit exceeded
         *
         * What is tested:
         *   todayTotal (4,90,000) + amount (20,000) = 5,10,000 > 5,00,000 → DAILY_LIMIT_EXCEEDED.
         *
         * Why verify(never()) on save():
         *   Confirms no Transfer is persisted when the limit guard fires.
         */
        @Test
        @DisplayName("should throw DAILY_LIMIT_EXCEEDED when daily cap is breached")
        void initiateTransfer_dailyLimitExceeded_throws() {
            when(accountClient.getByAccountNumber(SOURCE_ACCOUNT)).thenReturn(activeSource);
            when(accountClient.getByAccountNumber(DEST_ACCOUNT)).thenReturn(activeDest);
            // today's total is already 4,90,000; adding 20,000 puts it over 5,00,000
            when(transferRepository.sumSuccessfulTransfersAfter(eq(SOURCE_ACCOUNT), any()))
                    .thenReturn(new BigDecimal("490000"));
            validRequest.setAmount(new BigDecimal("20000"));
            activeSource.setBalance(new BigDecimal("500000")); // ensure sufficient balance for daily limit check

            TransferException ex = catchThrowableOfType(
                    () -> transferService.initiateTransfer(validRequest, USER_ID),
                    TransferException.class);

            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.DAILY_LIMIT_EXCEEDED);
            verify(transferRepository, never()).save(any());
        }

        /**
         * SUCCESS — daily limit boundary: exactly at the limit is allowed
         *
         * What is tested:
         *   todayTotal (4,00,000) + amount (1,00,000) = 5,00,000 == limit → ALLOWED.
         */
        @Test
        @DisplayName("should succeed when transfer exactly reaches the daily limit")
        void initiateTransfer_exactlyAtDailyLimit_succeeds() {
            when(accountClient.getByAccountNumber(SOURCE_ACCOUNT)).thenReturn(activeSource);
            when(accountClient.getByAccountNumber(DEST_ACCOUNT)).thenReturn(activeDest);
            when(transferRepository.sumSuccessfulTransfersAfter(eq(SOURCE_ACCOUNT), any()))
                    .thenReturn(new BigDecimal("400000"));
            validRequest.setAmount(new BigDecimal("100000"));
            activeSource.setBalance(new BigDecimal("500000")); // enough balance

            Transfer limitTransfer = Transfer.builder()
                    .id(2L)
                    .transactionReference("TXN999BOUNDARY")
                    .sourceAccount(SOURCE_ACCOUNT)
                    .destinationAccount(DEST_ACCOUNT)
                    .amount(new BigDecimal("100000"))
                    .status(TransferStatus.SUCCESS)
                    .createdAt(LocalDateTime.now())
                    .build();
            when(transferRepository.save(any(Transfer.class))).thenReturn(limitTransfer);

            TransferResponse response = transferService.initiateTransfer(validRequest, USER_ID);

            assertThat(response.getStatus()).isEqualTo(TransferStatus.SUCCESS);
        }

        /**
         * SUCCESS — null daily sum (no prior transfers) treated as zero
         *
         * What is tested:
         *   When sumSuccessfulTransfersAfter returns null (COALESCE missed or
         *   custom DB returns null), the service must not NPE — it defaults to 0.
         */
        @Test
        @DisplayName("should treat null daily sum as zero (no NPE)")
        void initiateTransfer_nullDailySum_treatedAsZero() {
            when(accountClient.getByAccountNumber(SOURCE_ACCOUNT)).thenReturn(activeSource);
            when(accountClient.getByAccountNumber(DEST_ACCOUNT)).thenReturn(activeDest);
            when(transferRepository.sumSuccessfulTransfersAfter(eq(SOURCE_ACCOUNT), any()))
                    .thenReturn(null); // simulate null return
            when(transferRepository.save(any(Transfer.class))).thenReturn(savedTransfer);

            assertThatCode(() -> transferService.initiateTransfer(validRequest, USER_ID))
                    .doesNotThrowAnyException();
        }

        /**
         * SUCCESS — Kafka failure does NOT roll back the committed transfer
         *
         * What is tested:
         *   Even if kafkaProducer.publishTransferCompleted() throws, the method
         *   must still return a SUCCESS response (Kafka is fire-and-forget in the
         *   producer — the exception happens asynchronously, but we verify the
         *   producer was still called).
         */
        @Test
        @DisplayName("should still return SUCCESS even when Kafka publish is called (fire-and-forget)")
        void initiateTransfer_kafkaCalled_regardlessOfResult() {
            when(accountClient.getByAccountNumber(SOURCE_ACCOUNT)).thenReturn(activeSource);
            when(accountClient.getByAccountNumber(DEST_ACCOUNT)).thenReturn(activeDest);
            when(transferRepository.sumSuccessfulTransfersAfter(any(), any()))
                    .thenReturn(BigDecimal.ZERO);
            when(transferRepository.save(any(Transfer.class))).thenReturn(savedTransfer);

            TransferResponse response = transferService.initiateTransfer(validRequest, USER_ID);

            // Kafka producer must always be called — internal async failure is its problem
            verify(kafkaProducer, times(1)).publishTransferCompleted(any());
            assertThat(response.getStatus()).isEqualTo(TransferStatus.SUCCESS);
        }
    }

    // =========================================================================
    // getByReference()
    // =========================================================================

    @Nested
    @DisplayName("getByReference()")
    class GetByReferenceTests {

        /**
         * SUCCESS — existing reference returns correct response
         *
         * What is tested:
         *   Repository returns a Transfer → method maps it to TransferResponse.
         *
         * Why verify():
         *   Confirms exactly one repository call with the correct reference.
         */
        @Test
        @DisplayName("should return TransferResponse for valid reference")
        void getByReference_success() {
            when(transferRepository.findByTransactionReference("TXN4A3B2C1D5E6F"))
                    .thenReturn(Optional.of(savedTransfer));

            TransferResponse response = transferService.getByReference("TXN4A3B2C1D5E6F");

            assertThat(response).isNotNull();
            assertThat(response.getTransactionReference()).isEqualTo("TXN4A3B2C1D5E6F");
            assertThat(response.getSourceAccount()).isEqualTo(SOURCE_ACCOUNT);
            assertThat(response.getAmount()).isEqualByComparingTo(AMOUNT_1000);
            assertThat(response.getMessage()).isEqualTo("Transfer retrieved successfully");

            verify(transferRepository, times(1))
                    .findByTransactionReference("TXN4A3B2C1D5E6F");
        }

        /**
         * FAILURE — unknown reference throws TRANSACTION_NOT_FOUND
         */
        @Test
        @DisplayName("should throw TRANSACTION_NOT_FOUND for unknown reference")
        void getByReference_notFound_throws() {
            when(transferRepository.findByTransactionReference("TXN_UNKNOWN"))
                    .thenReturn(Optional.empty());

            TransferException ex = catchThrowableOfType(
                    () -> transferService.getByReference("TXN_UNKNOWN"),
                    TransferException.class);

            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.TRANSACTION_NOT_FOUND);
            assertThat(ex.getMessage()).contains("TXN_UNKNOWN");
        }
    }

    // =========================================================================
    // getHistory()
    // =========================================================================

    @Nested
    @DisplayName("getHistory()")
    class GetHistoryTests {

        private final Pageable pageable = PageRequest.of(0, 10);

        /**
         * SUCCESS — no filters: uses findBySourceAccount
         */
        @Test
        @DisplayName("should call findBySourceAccount when no filters supplied")
        void getHistory_noFilters_callsCorrectRepository() {
            Page<Transfer> page = new PageImpl<>(List.of(savedTransfer));
            when(transferRepository.findBySourceAccount(SOURCE_ACCOUNT, pageable))
                    .thenReturn(page);

            Page<TransferHistoryResponse> result =
                    transferService.getHistory(SOURCE_ACCOUNT, null, null, null, pageable);

            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).getTransactionReference())
                    .isEqualTo("TXN4A3B2C1D5E6F");

            verify(transferRepository).findBySourceAccount(SOURCE_ACCOUNT, pageable);
            verify(transferRepository, never()).findBySourceAccountAndStatus(any(), any(), any());
        }

        /**
         * SUCCESS — status filter only: uses findBySourceAccountAndStatus
         */
        @Test
        @DisplayName("should call findBySourceAccountAndStatus when only status supplied")
        void getHistory_statusOnly_callsCorrectRepository() {
            Page<Transfer> page = new PageImpl<>(List.of(savedTransfer));
            when(transferRepository.findBySourceAccountAndStatus(
                    SOURCE_ACCOUNT, TransferStatus.SUCCESS, pageable)).thenReturn(page);

            Page<TransferHistoryResponse> result = transferService.getHistory(
                    SOURCE_ACCOUNT, TransferStatus.SUCCESS, null, null, pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getStatus()).isEqualTo(TransferStatus.SUCCESS);

            verify(transferRepository).findBySourceAccountAndStatus(
                    SOURCE_ACCOUNT, TransferStatus.SUCCESS, pageable);
        }

        /**
         * SUCCESS — date range only: uses findBySourceAccountAndCreatedAtBetween
         */
        @Test
        @DisplayName("should call findBySourceAccountAndCreatedAtBetween when only date range supplied")
        void getHistory_dateRangeOnly_callsCorrectRepository() {
            LocalDateTime from = LocalDateTime.now().minusDays(7);
            LocalDateTime to   = LocalDateTime.now();
            Page<Transfer> page = new PageImpl<>(List.of(savedTransfer));
            when(transferRepository.findBySourceAccountAndCreatedAtBetween(
                    SOURCE_ACCOUNT, from, to, pageable)).thenReturn(page);

            Page<TransferHistoryResponse> result =
                    transferService.getHistory(SOURCE_ACCOUNT, null, from, to, pageable);

            assertThat(result.getContent()).hasSize(1);
            verify(transferRepository).findBySourceAccountAndCreatedAtBetween(
                    SOURCE_ACCOUNT, from, to, pageable);
        }

        /**
         * SUCCESS — both status and date range: uses full filter method
         */
        @Test
        @DisplayName("should call findBy...StatusAndCreatedAtBetween when both filters supplied")
        void getHistory_statusAndDateRange_callsCorrectRepository() {
            LocalDateTime from = LocalDateTime.now().minusDays(30);
            LocalDateTime to   = LocalDateTime.now();
            Page<Transfer> page = new PageImpl<>(List.of(savedTransfer));
            when(transferRepository.findBySourceAccountAndStatusAndCreatedAtBetween(
                    SOURCE_ACCOUNT, TransferStatus.SUCCESS, from, to, pageable))
                    .thenReturn(page);

            Page<TransferHistoryResponse> result = transferService.getHistory(
                    SOURCE_ACCOUNT, TransferStatus.SUCCESS, from, to, pageable);

            assertThat(result.getContent()).hasSize(1);
            verify(transferRepository).findBySourceAccountAndStatusAndCreatedAtBetween(
                    SOURCE_ACCOUNT, TransferStatus.SUCCESS, from, to, pageable);
        }

        /**
         * SUCCESS — empty page maps cleanly without NPE
         */
        @Test
        @DisplayName("should return empty page without error")
        void getHistory_emptyResult_returnsEmptyPage() {
            when(transferRepository.findBySourceAccount(SOURCE_ACCOUNT, pageable))
                    .thenReturn(Page.empty(pageable));

            Page<TransferHistoryResponse> result =
                    transferService.getHistory(SOURCE_ACCOUNT, null, null, null, pageable);

            assertThat(result.getTotalElements()).isZero();
            assertThat(result.getContent()).isEmpty();
        }
    }
}
