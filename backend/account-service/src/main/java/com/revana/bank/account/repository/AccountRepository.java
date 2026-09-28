package com.revana.bank.account.repository;

import com.revana.bank.account.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AccountRepository
        extends JpaRepository<Account, Long> {

    Optional<Account> findByAccountNumber(String accountNumber);

    Optional<Account> findByUserId(Long userId);

    List<Account> findByCustomerNameContainingIgnoreCase(String name);
    boolean existsByCustomerId(
            String customerId);
}