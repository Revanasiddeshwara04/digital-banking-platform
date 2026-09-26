package com.revana.bank.account.controller;

import com.revana.bank.account.dto.*;
import com.revana.bank.account.entity.Account;
import com.revana.bank.account.entity.Transaction;
import com.revana.bank.account.service.AccountService;
import com.revana.bank.account.service.PdfService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService service;
    private final PdfService pdfService;

    public AccountController(AccountService service,
                             PdfService pdfService) {
        this.service = service;
        this.pdfService = pdfService;
    }

    @GetMapping("/test-rate")
    public String testRate() {

        return service.testLimiter();
    }

    @PostMapping
    public Account createAccount(
            @RequestBody CreateAccountRequest request) {

        return service.createAccount(request);
    }

    @GetMapping("/{id}")
    public Account getAccount(
            @PathVariable Long id,
            @RequestHeader("X-Username") String username,
            @RequestHeader("X-Role") String role) {

        System.out.println("USERNAME = " + username);
        System.out.println("ROLE = " + role);

        return service.getAccount(id);
    }

    @GetMapping("/{id}/statement/pdf")
    public ResponseEntity<byte[]> downloadStatement(
            @PathVariable Long id)
            throws Exception {

        AccountStatementResponse statement =
                service.getStatement(id);

        byte[] pdf =
                pdfService.generateStatement(statement);

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=statement.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping
    public List<Account> getAllAccounts() {

        return service.getAllAccounts();
    }

    @PostMapping("/{id}/deposit")
    public Account deposit(
            @PathVariable Long id,
            @RequestBody AmountRequest request,
            @RequestHeader("X-Username") String username,
            @RequestHeader("X-Role") String role,
            @RequestHeader("X-UserId") Long userId) {

        return service.deposit(
                id,
                request.getAmount(),
                userId,
                role);
    }

    @PostMapping("/{id}/withdraw")
    public Account withdraw(
            @PathVariable Long id,
            @RequestBody AmountRequest request) {

        return service.withdraw(id, request.getAmount());
    }
    @PostMapping("/transfer")
    public TransferResponse transfer(
            @RequestBody TransferRequest request) {

        return service.transfer(request);
    }

    @GetMapping("/{id}/transactions")
    public List<TransactionResponse> getTransactions(
            @PathVariable Long id) {

        return service.getTransactions(id);
    }

    @GetMapping("/{id}/statement")
    public AccountStatementResponse getStatement(
            @PathVariable Long id) {

        return service.getStatement(id);
    }
    @PutMapping("/{id}/close")
    public Account closeAccount(
            @PathVariable Long id) {

        return service.closeAccount(id);
    }
    @GetMapping("/search/name")
    public List<Account> searchByName(
            @RequestParam String name) {

        return service.searchByName(name);
    }

    @GetMapping("/search/account-number")
    public Account searchByAccountNumber(
            @RequestParam String accountNumber) {

        return service.searchByAccountNumber(accountNumber);
    }

    @GetMapping("/user/{userId}")
    public Account getByUserId(
            @PathVariable Long userId) {

        return service.getByUserId(userId);
    }

    @GetMapping("/my-account")
    public Account getMyAccount(
            @RequestHeader("Authorization")
            String token) {

        return service.getMyAccount(token);
    }
}