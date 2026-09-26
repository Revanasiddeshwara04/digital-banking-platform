package com.revana.bank.customer.repository;

import com.revana.bank.customer.entity.AddressType;
import com.revana.bank.customer.entity.CustomerAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerAddressRepository
        extends JpaRepository<CustomerAddress, Long> {

    /**
     * Get all addresses for a customer
     */
    List<CustomerAddress> findByCustomerId(
            Long customerId
    );

    /**
     * Get address by customer and type
     */
    Optional<CustomerAddress> findByCustomerIdAndAddressType(
            Long customerId,
            AddressType addressType
    );

    /**
     * Check if address type already exists
     */
    boolean existsByCustomerIdAndAddressType(
            Long customerId,
            AddressType addressType
    );

    /**
     * Delete all addresses of a customer
     */
    void deleteByCustomerId(
            Long customerId
    );

    /**
     * Get all permanent addresses
     */
    List<CustomerAddress> findByAddressType(
            AddressType addressType
    );
}