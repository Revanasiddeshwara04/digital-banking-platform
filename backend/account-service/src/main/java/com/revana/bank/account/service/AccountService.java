package com.revana.bank.account.service;

import com.revana.bank.account.client.AuthClient;
import com.revana.bank.account.client.NotificationClient;
import com.revana.bank.account.client.TransactionClient;
import com.revana.bank.account.config.RateLimiterService;
import com.revana.bank.account.dto.*;
import com.revana.bank.account.entity.Account;
import com.revana.bank.account.entity.Transaction;
import com.revana.bank.account.exception.AccessDeniedException;
import com.revana.bank.account.exception.AccountNotFoundException;
import com.revana.bank.account.exception.RateLimitExceededException;
import com.revana.bank.account.kafka.AuditProducer;
import com.revana.bank.account.repository.AccountRepository;
import com.revana.bank.account.repository.TransactionRepository;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import org.apache.kafka.common.errors.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.CacheEvict;
import com.revana.bank.account.dto.TransactionEvent;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;



import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class AccountService {

    private final AccountRepository repository;
    private final TransactionRepository transactionRepository;
//    private final TransactionClient transactionClient;
private final TransactionClientService transactionService;
    private final NotificationClient notificationClient;
    private final KafkaProducerService kafkaProducer;
    private final TransactionEventProducer eventProducer;
    private final NotificationService notificationService;
    private final NotificationEventProducer notificationProducer;
    private final AuthClient authClient;
    private final RateLimiterService rateLimiterService;
    private final AuditProducer auditProducer;


    public AccountService(
            AccountRepository repository,
            TransactionRepository transactionRepository,
//            TransactionClient transactionClient,
            TransactionClientService transactionService,
            NotificationClient notificationClient,
            NotificationService notificationService,
            KafkaProducerService kafkaProducer,
            TransactionEventProducer eventProducer,
            NotificationEventProducer notificationProducer,
            AuthClient authClient,

            RateLimiterService rateLimiterService, AuditProducer auditProducer) {

        this.repository = repository;
        this.transactionRepository = transactionRepository;
        this.transactionService = transactionService;
//        this.transactionClient = transactionClient;
        this.notificationClient = notificationClient;
        this.notificationService = notificationService;
        this.kafkaProducer = kafkaProducer;
        this.eventProducer = eventProducer;
        this.notificationProducer = notificationProducer;
        this.authClient = authClient;
        this.rateLimiterService = rateLimiterService;
        this.auditProducer = auditProducer;
    }


    public Account createAccount(
            CreateAccountRequest request) {

        if(repository.existsByCustomerId(
                request.getCustomerId())) {

            throw new RuntimeException(
                    "Account already exists for customer");
        }

        System.out.println("CREATE ACCOUNT STARTED");

        Account account = Account.builder()
                .userId(request.getUserId())
                .customerId(request.getCustomerId())
                .accountNumber(generateAccountNumber())
                .customerName(request.getCustomerName())
                .accountType(request.getAccountType())
                .balance(request.getBalance())
                .status("ACTIVE")
                .createdAt(LocalDateTime.now())
                .build();

        System.out.println("ACCOUNT OBJECT CREATED");

        Account savedAccount =
                repository.save(account);

        auditProducer.publish(
                AuditEvent.builder()
                        .auditId(java.util.UUID.randomUUID().toString())
                        .eventType("ACCOUNT_CREATED")
                        .serviceName("ACCOUNT-SERVICE")
                        .entityId(savedAccount.getAccountNumber())
                        .performedBy(savedAccount.getCustomerName())
                        .actionStatus("SUCCESS")
                        .eventTimestamp(LocalDateTime.now())
                        .payload(savedAccount.getCustomerId())
                        .build()
        );

        return savedAccount;

    }

    public Account getMyAccount(
            String token) {

        UserResponse user =
                authClient.getCurrentUser(token);

        return repository.findByUserId(
                        user.getId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Account not found"));
    }



    @Cacheable(
            value = "accounts",
            key = "#id")
    public Account getAccount(Long id) {

        System.out.println(
                "Fetching from Database...");

        return repository.findById(id)
                .orElseThrow(() ->
                        new AccountNotFoundException(
                                "Account Not Found"));
    }

    public List<Account> getAllAccounts() {
        return repository.findAll();
    }

    private String generateAccountNumber() {

        return "ACC" +
                UUID.randomUUID()
                        .toString()
                        .substring(0, 6)
                        .toUpperCase();
    }

    public Account depositRateLimitFallback(
            Long id,
            BigDecimal amount,
            RequestNotPermitted ex) {

        System.out.println("RATE LIMIT HIT");

        throw new RateLimitExceededException(
                "Too many deposit requests. Please try again later.");
    }

    public Account withdrawRateLimitFallback(
            Long id,
            BigDecimal amount,
            RequestNotPermitted ex) {

        throw new RateLimitExceededException(
                "Too many withdrawal requests. Please try again later.");
    }

    public TransferResponse transferRateLimitFallback(
            TransferRequest request,
            RequestNotPermitted ex) {

        throw new RateLimitExceededException(
                "Too many transfer requests. Please try again later.");
    }

    @RateLimiter(
            name = "depositLimiter",
            fallbackMethod = "depositRateLimitFallback")
    @Transactional
    @CachePut(
            value = "accounts",
            key = "#id")
    public Account deposit(
            Long id,
            BigDecimal amount,
            Long userId,
            String role) {
        System.out.println("DEPOSIT EXECUTED");

        if (amount == null ||
                amount.compareTo(BigDecimal.ZERO) <= 0) {

            throw new RuntimeException(
                    "Deposit amount must be greater than zero");
        }
                 Account account = repository.findById(id)
                .orElseThrow(() ->
                        new AccountNotFoundException("Account Not Found"));


        if ("CUSTOMER".equals(role)
                && !account.getUserId().equals(userId)) {

            throw new AccessDeniedException(
                    "You are not allowed to access this account");
        }

        account.setBalance(
                account.getBalance().add(amount));

        Account updatedAccount = repository.save(account);

        auditProducer.publish(
                AuditEvent.builder()
                        .auditId(UUID.randomUUID().toString())
                        .eventType("ACCOUNT_DEPOSIT")
                        .serviceName("ACCOUNT-SERVICE")
                        .entityId(updatedAccount.getAccountNumber())
                        .performedBy(updatedAccount.getCustomerName())
                        .actionStatus("SUCCESS")
                        .eventTimestamp(LocalDateTime.now())
                        .payload(amount.toString())
                        .build()
        );

        TransactionRequest txRequest =
                TransactionRequest.builder()
                        .fromAccountId(id)
                        .toAccountId(id)
                        .transactionType("DEPOSIT")
                        .amount(amount)
                        .status("SUCCESS")
                        .build();

        transactionService.createTransaction(txRequest);



        NotificationRequest notification =
                NotificationRequest.builder()
                        .accountId(id)
                        .message(
                                "₹" + amount +
                                        " deposited successfully")
                        .notificationType("EMAIL")
                        .build();

        notificationService.sendNotification(notification);

        try {

            TransactionEvent event =
                    TransactionEvent.builder()
                            .accountNumber(
                                    account.getAccountNumber())
                            .transactionType("DEPOSIT")
                            .amount(amount)
                            .build();

            eventProducer.publish(event);

        } catch (Exception e) {

            System.out.println("KAFKA ERROR = "
                    + e.getMessage());
        }

        return updatedAccount;
    }


    @Transactional
    @CachePut(
            value = "accounts",
            key = "#id")
    @RateLimiter(
            name = "withdrawLimiter",
            fallbackMethod = "withdrawRateLimitFallback")
    public Account withdraw(
            Long id,
            BigDecimal amount) {

        System.out.println("WITHDRAW EXECUTED");

        if (amount == null ||
                amount.compareTo(BigDecimal.ZERO) <= 0) {

            throw new RuntimeException(
                    "Withdrawal amount must be greater than zero");
        }

        Account account = repository.findById(id)
                .orElseThrow(() ->
                        new AccountNotFoundException(
                                "Account Not Found"));

        if (account.getBalance()
                .compareTo(amount) < 0) {

            throw new RuntimeException(
                    "Insufficient Balance");
        }

        account.setBalance(
                account.getBalance()
                        .subtract(amount));

        Account updatedAccount =
                repository.save(account);

        auditProducer.publish(
                AuditEvent.builder()
                        .auditId(UUID.randomUUID().toString())
                        .eventType("ACCOUNT_WITHDRAW")
                        .serviceName("ACCOUNT-SERVICE")
                        .entityId(updatedAccount.getAccountNumber())
                        .performedBy(updatedAccount.getCustomerName())
                        .actionStatus("SUCCESS")
                        .eventTimestamp(LocalDateTime.now())
                        .payload(amount.toString())
                        .build()
        );

        TransactionRequest txRequest =
                TransactionRequest.builder()
                        .fromAccountId(id)
                        .toAccountId(id)
                        .transactionType("WITHDRAW")
                        .amount(amount)
                        .status("SUCCESS")
                        .build();

        transactionService.createTransaction(
                txRequest);

        NotificationRequest notification =
                NotificationRequest.builder()
                        .accountId(id)
                        .message(
                                "₹" + amount +
                                        " withdrawn successfully")
                        .notificationType("EMAIL")
                        .build();

        notificationService.sendNotification(
                notification);

        try {

            TransactionEvent event =
                    TransactionEvent.builder()
                            .accountNumber(
                                    account.getAccountNumber())
                            .transactionType("WITHDRAW")
                            .amount(amount)
                            .build();

            eventProducer.publish(event);

        } catch (Exception e) {

            System.out.println(
                    "KAFKA ERROR = "
                            + e.getMessage());
        }

        return updatedAccount;
    }


    @Transactional
    @RateLimiter(
            name = "transferLimiter",
            fallbackMethod = "transferRateLimitFallback")
    public TransferResponse transfer(TransferRequest request) {

        if (request.getAmount() == null ||
                request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {

            throw new RuntimeException(
                    "Transfer amount must be greater than zero");
        }

        System.out.println("STEP 1");

        Account sender = repository.findById(
                        request.getFromAccountId())
                .orElseThrow(() ->
                        new AccountNotFoundException(
                                "Sender Account Not Found"));

        System.out.println("STEP 2");
        Account receiver = repository.findById(
                        request.getToAccountId())
                .orElseThrow(() ->
                        new AccountNotFoundException(
                                "Receiver Account Not Found"));
        System.out.println("STEP 3");

        if (sender.getBalance()
                .compareTo(request.getAmount()) < 0) {

            throw new RuntimeException(
                    "Insufficient Balance");
        }

        sender.setBalance(
                sender.getBalance()
                        .subtract(request.getAmount()));

        receiver.setBalance(
                receiver.getBalance()
                        .add(request.getAmount()));

        System.out.println("STEP 4");

        repository.save(sender);
        repository.save(receiver);

        auditProducer.publish(
                AuditEvent.builder()
                        .auditId(UUID.randomUUID().toString())
                        .eventType("ACCOUNT_TRANSFER")
                        .serviceName("ACCOUNT-SERVICE")
                        .entityId(sender.getAccountNumber())
                        .performedBy(sender.getCustomerName())
                        .actionStatus("SUCCESS")
                        .eventTimestamp(LocalDateTime.now())
                        .payload(
                                request.getAmount() +
                                        " transferred to " +
                                        receiver.getAccountNumber()
                        )
                        .build()
        );

        TransactionRequest txRequest =
                TransactionRequest.builder()
                        .fromAccountId(sender.getId())
                        .toAccountId(receiver.getId())
                        .transactionType("TRANSFER")
                        .amount(request.getAmount())
                        .status("SUCCESS")
                        .build();
        System.out.println("STEP 5");

        transactionService .createTransaction(txRequest);
        System.out.println("STEP 6");

        NotificationRequest senderNotification =
                NotificationRequest.builder()
                        .accountId(sender.getId())
                        .message(
                                "₹" + request.getAmount() +
                                        " transferred to " +
                                        receiver.getCustomerName())
                        .notificationType("EMAIL")
                        .build();

        System.out.println("STEP 6");
        notificationService.sendNotification(senderNotification);

        NotificationRequest receiverNotification =
                NotificationRequest.builder()
                        .accountId(receiver.getId())
                        .message(
                                "₹" + request.getAmount() +
                                        " received from " +
                                        sender.getCustomerName())
                        .notificationType("EMAIL")
                        .build();

        System.out.println("STEP 7");
        notificationService.sendNotification(receiverNotification);
        System.out.println("STEP 8");

        try {

            TransferEvent event =
                    TransferEvent.builder()
                            .senderId(sender.getId())
                            .receiverId(receiver.getId())
                            .senderName(sender.getCustomerName())
                            .receiverName(receiver.getCustomerName())
                            .amount(request.getAmount())
                            .build();

            kafkaProducer.publishTransferEvent(
                    event);

        } catch (Exception e) {

            System.out.println(
                    "KAFKA ERROR = "
                            + e.getMessage());
        }



        return TransferResponse.builder()
                .senderName(sender.getCustomerName())
                .receiverName(receiver.getCustomerName())
                .senderAccountNumber(sender.getAccountNumber())
                .receiverAccountNumber(receiver.getAccountNumber())
                .transferAmount(request.getAmount())
                .senderBalance(sender.getBalance())
                .receiverBalance(receiver.getBalance())
                .message("Transfer Successful")
                .build();
    }

    public List<TransactionResponse> getTransactions(Long accountId) {

        List<Transaction> transactions =
                transactionRepository
                        .findByFromAccountIdOrToAccountId(
                                accountId,
                                accountId);

        return transactions.stream()
                .map(tx -> {

                    Account sender = repository.findById(
                                    tx.getFromAccountId())
                            .orElse(null);

                    Account receiver = repository.findById(
                                    tx.getToAccountId())
                            .orElse(null);

                    return TransactionResponse.builder()
                            .senderName(sender != null ? sender.getCustomerName() : "")
                            .receiverName(receiver != null ? receiver.getCustomerName() : "")
                            .senderAccountNumber(sender != null ? sender.getAccountNumber() : "")
                            .receiverAccountNumber(receiver != null ? receiver.getAccountNumber() : "")
                            .transactionType(tx.getTransactionType())
                            .amount(tx.getAmount())
                            .status(tx.getStatus())
                            .transactionDate(tx.getTransactionDate())
                            .build();
                })
                .toList();
    }

    public AccountStatementResponse getStatement(Long accountId) {

        Account account = repository.findById(accountId)
                .orElseThrow(() ->
                        new AccountNotFoundException(
                                "Account Not Found"));

        List<TransactionResponse> transactions =
                getTransactions(accountId);

        return AccountStatementResponse.builder()
                .accountNumber(account.getAccountNumber())
                .customerName(account.getCustomerName())
                .currentBalance(account.getBalance())
                .transactions(transactions)
                .build();
    }

    @Transactional
    @CacheEvict(
            value = "accounts",
            key = "#id")
//    @RateLimiter(
//            name = "accountRateLimiter",
//            fallbackMethod = "depositRateLimitFallback")
    public Account closeAccount(Long id) {

        Account account = repository.findById(id)
                .orElseThrow(() ->
                        new AccountNotFoundException(
                                "Account Not Found"));

        account.setStatus("CLOSED");
        account.setClosedAt(LocalDateTime.now());

        Account savedAccount =
                repository.save(account);

        auditProducer.publish(
                AuditEvent.builder()
                        .auditId(UUID.randomUUID().toString())
                        .eventType("ACCOUNT_CLOSED")
                        .serviceName("ACCOUNT-SERVICE")
                        .entityId(savedAccount.getAccountNumber())
                        .performedBy(savedAccount.getCustomerName())
                        .actionStatus("SUCCESS")
                        .eventTimestamp(LocalDateTime.now())
                        .payload("Account Closed")
                        .build()
        );

        return savedAccount;
    }
    public List<Account> searchByName(String name) {
        return repository
                .findByCustomerNameContainingIgnoreCase(name);
    }

    public Account searchByAccountNumber(String accountNumber) {

        return repository
                .findByAccountNumber(accountNumber)
                .orElseThrow(() ->
                        new AccountNotFoundException(
                                "Account Not Found"));
    }


    @RateLimiter(
            name = "accountRateLimiter",
            fallbackMethod = "testFallback")
    public String testLimiter() {

        System.out.println("TEST EXECUTED");

        return "SUCCESS";
    }

    public String testFallback(RequestNotPermitted ex) {

        System.out.println("RATE LIMIT HIT");

        return "LIMIT EXCEEDED";
    }

    public Account getByUserId(Long userId) {

        return repository.findByUserId(userId)
                .orElseThrow(() ->
                        new AccountNotFoundException(
                                "Account not found for user id: " + userId));
    }


    }
