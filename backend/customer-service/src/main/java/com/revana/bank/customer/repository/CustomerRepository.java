package com.revana.bank.customer.repository;

import com.revana.bank.customer.entity.Customer;
import com.revana.bank.customer.entity.CustomerStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository
        extends JpaRepository<Customer, Long> {

    Optional<Customer> findByCustomerId(
            String customerId
    );

    Optional<Customer> findByCifNumber(
            String cifNumber
    );

    Optional<Customer> findByEmail(
            String email
    );

    Optional<Customer> findByMobileNumber(
            String mobileNumber
    );

    boolean existsByEmail(
            String email
    );

    boolean existsByMobileNumber(
            String mobileNumber
    );

    boolean existsByCustomerId(
            String customerId
    );

    boolean existsByCifNumber(
            String cifNumber
    );

    List<Customer> findByStatus(
            CustomerStatus status
    );

    List<Customer> findByFirstNameContainingIgnoreCase(
            String firstName
    );

    List<Customer> findByLastNameContainingIgnoreCase(
            String lastName
    );

    List<Customer> findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(
            String firstName,
            String lastName
    );
}