package com.revana.bank.customer.controller;

import com.revana.bank.customer.dto.AddressRequest;
import com.revana.bank.customer.dto.AddressResponse;
import com.revana.bank.customer.dto.CustomerRequest;
import com.revana.bank.customer.dto.CustomerResponse;
import com.revana.bank.customer.service.CustomerService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    /**
     * CREATE CUSTOMER
     */
    @PostMapping
    public ResponseEntity<CustomerResponse> createCustomer(
            @Valid @RequestBody CustomerRequest request) {

        CustomerResponse response =
                customerService.createCustomer(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(response);
    }

    /**
     * GET CUSTOMER BY CUSTOMER ID
     */
    @GetMapping("/{customerId}")
    public ResponseEntity<CustomerResponse> getCustomer(
            @PathVariable String customerId) {

        return ResponseEntity.ok(
                customerService.getCustomerByCustomerId(
                        customerId));
    }

    /**
     * GET CUSTOMER BY CIF
     */
    @GetMapping("/cif/{cifNumber}")
    public ResponseEntity<CustomerResponse> getCustomerByCif(
            @PathVariable String cifNumber) {

        return ResponseEntity.ok(
                customerService.getCustomerByCif(
                        cifNumber));
    }

    /**
     * UPDATE CUSTOMER
     */
    @PutMapping("/{customerId}")
    public ResponseEntity<CustomerResponse> updateCustomer(
            @PathVariable String customerId,
            @Valid @RequestBody CustomerRequest request) {

        return ResponseEntity.ok(
                customerService.updateCustomer(
                        customerId,
                        request));
    }

    /**
     * DELETE CUSTOMER
     */
    @DeleteMapping("/{customerId}")
    public ResponseEntity<String> deleteCustomer(
            @PathVariable String customerId) {

        customerService.deleteCustomer(customerId);

        return ResponseEntity.ok(
                "Customer deleted successfully");
    }

    /**
     * ADD CUSTOMER ADDRESS
     */
    @PostMapping("/{customerId}/addresses")
    public ResponseEntity<AddressResponse> addAddress(
            @PathVariable String customerId,
            @Valid @RequestBody AddressRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(customerService.addAddress(
                        customerId,
                        request));
    }

    /**
     * GET CUSTOMER ADDRESSES
     */
    @GetMapping("/{customerId}/addresses")
    public ResponseEntity<List<AddressResponse>>
    getCustomerAddresses(
            @PathVariable String customerId) {

        return ResponseEntity.ok(
                customerService.getCustomerAddresses(
                        customerId));
    }

    /**
     * GET ALL CUSTOMERS
     */
    @GetMapping
    public ResponseEntity<List<CustomerResponse>>
    getAllCustomers() {

        return ResponseEntity.ok(
                customerService.getAllCustomers());
    }

    /**
     * ACTIVATE CUSTOMER
     */
    @PutMapping("/{customerId}/activate")
    public ResponseEntity<CustomerResponse>
    activateCustomer(
            @PathVariable String customerId) {

        return ResponseEntity.ok(
                customerService.activateCustomer(
                        customerId));
    }

    /**
     * DEACTIVATE CUSTOMER
     */
    @PutMapping("/{customerId}/deactivate")
    public ResponseEntity<CustomerResponse>
    deactivateCustomer(
            @PathVariable String customerId) {

        return ResponseEntity.ok(
                customerService.deactivateCustomer(
                        customerId));
    }


}