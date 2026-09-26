package com.revana.bank.customer.service;

import com.revana.bank.customer.dto.*;
import com.revana.bank.customer.entity.Customer;
import com.revana.bank.customer.entity.CustomerAddress;
import com.revana.bank.customer.entity.CustomerStatus;
import com.revana.bank.customer.kafka.event.CustomerCreatedEvent;
import com.revana.bank.customer.kafka.producer.CustomerEventProducer;
import com.revana.bank.customer.repository.CustomerAddressRepository;
import com.revana.bank.customer.repository.CustomerRepository;
import com.revana.bank.customer.util.CIFGenerator;
import com.revana.bank.customer.exception.CustomerNotFoundException;
import com.revana.bank.customer.kafka.CustomerActivatedEvent;
import com.revana.bank.customer.kafka.CustomerKafkaProducer;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CustomerService {

    private final CustomerRepository customerRepository;

    private final CustomerAddressRepository customerAddressRepository;

    private final CIFGenerator cifGenerator;

    private final CustomerEventProducer customerEventProducer;
    private final CustomerKafkaProducer customerKafkaProducer;


    /**
     * CREATE CUSTOMER
     */
    public CustomerResponse createCustomer(
            CustomerRequest request) {

        validateDuplicateCustomer(request);

        Customer customer = Customer.builder()
                .customerId(
                        cifGenerator.generateCustomerId())
                .cifNumber(
                        cifGenerator.generateCifNumber())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .dateOfBirth(request.getDateOfBirth())
                .mobileNumber(request.getMobileNumber())
                .email(request.getEmail())
                .status(CustomerStatus.PENDING_KYC)
                .build();

        Customer savedCustomer =
                customerRepository.save(customer);

        publishCustomerCreatedEvent(savedCustomer);

        return mapToResponse(savedCustomer);
    }

    /**
     * GET CUSTOMER BY CUSTOMER ID
     */
    @Transactional(readOnly = true)
    public CustomerResponse getCustomerByCustomerId(
            String customerId) {

        Customer customer =
                customerRepository
                        .findByCustomerId(customerId)
                        .orElseThrow(() ->
                                new CustomerNotFoundException(
                                        "Customer not found : "
                                                + customerId));

        return mapToResponse(customer);
    }

    /**
     * GET CUSTOMER BY CIF
     */
    @Transactional(readOnly = true)
    public CustomerResponse getCustomerByCif(
            String cifNumber) {

        Customer customer =
                customerRepository
                        .findByCifNumber(cifNumber)
                        .orElseThrow(() ->
                                new CustomerNotFoundException(
                                        "Customer not found : "
                                                + cifNumber));

        return mapToResponse(customer);
    }

    /**
     * UPDATE CUSTOMER
     */
    public CustomerResponse updateCustomer(
            String customerId,
            CustomerRequest request) {

        Customer customer =
                customerRepository
                        .findByCustomerId(customerId)
                        .orElseThrow(() ->
                                new CustomerNotFoundException(
                                        "Customer not found : "
                                                + customerId));

        customer.setFirstName(
                request.getFirstName());

        customer.setLastName(
                request.getLastName());

        customer.setDateOfBirth(
                request.getDateOfBirth());

        customer.setMobileNumber(
                request.getMobileNumber());

        customer.setEmail(
                request.getEmail());

        Customer updatedCustomer =
                customerRepository.save(customer);

        return mapToResponse(updatedCustomer);
    }

    /**
     * DELETE CUSTOMER
     */
    public void deleteCustomer(
            String customerId) {

        Customer customer =
                customerRepository
                        .findByCustomerId(customerId)
                        .orElseThrow(() ->
                                new CustomerNotFoundException(
                                        "Customer not found : "
                                                + customerId));

        customerRepository.delete(customer);
    }

    /**
     * ADD CUSTOMER ADDRESS
     */
    public AddressResponse addAddress(
            String customerId,
            AddressRequest request) {

        Customer customer =
                customerRepository
                        .findByCustomerId(customerId)
                        .orElseThrow(() ->
                                new CustomerNotFoundException(
                                        "Customer not found : "
                                                + customerId));

        if (customerAddressRepository
                .existsByCustomerIdAndAddressType(
                        customer.getId(),
                        request.getAddressType())) {

            throw new RuntimeException(
                    request.getAddressType()
                            + " address already exists");
        }

        CustomerAddress address =
                CustomerAddress.builder()
                        .customerId(customer.getId())
                        .addressType(
                                request.getAddressType())
                        .addressLine1(
                                request.getAddressLine1())
                        .addressLine2(
                                request.getAddressLine2())
                        .city(request.getCity())
                        .state(request.getState())
                        .country(request.getCountry())
                        .pinCode(request.getPinCode())
                        .build();

        CustomerAddress savedAddress =
                customerAddressRepository.save(address);

        return mapToAddressResponse(savedAddress);
    }

    /**
     * GET ALL CUSTOMER ADDRESSES
     */
    @Transactional(readOnly = true)
    public List<AddressResponse> getCustomerAddresses(
            String customerId) {

        Customer customer =
                customerRepository
                        .findByCustomerId(customerId)
                        .orElseThrow(() ->
                                new CustomerNotFoundException(
                                        "Customer not found : "
                                                + customerId));

        return customerAddressRepository
                .findByCustomerId(customer.getId())
                .stream()
                .map(this::mapToAddressResponse)
                .collect(Collectors.toList());
    }

    /**
     * VALIDATE DUPLICATES
     */
    private void validateDuplicateCustomer(
            CustomerRequest request) {

        if (customerRepository.existsByEmail(
                request.getEmail())) {

            throw new RuntimeException(
                    "Email already exists");
        }

        if (customerRepository
                .existsByMobileNumber(
                        request.getMobileNumber())) {

            throw new RuntimeException(
                    "Mobile number already exists");
        }
    }

    /**
     * PUBLISH EVENT
     */
    private void publishCustomerCreatedEvent(
            Customer customer) {

        CustomerCreatedEvent event =
                CustomerCreatedEvent.builder()
                        .customerId(
                                customer.getCustomerId())
                        .cifNumber(
                                customer.getCifNumber())
                        .firstName(
                                customer.getFirstName())
                        .lastName(
                                customer.getLastName())
                        .email(
                                customer.getEmail())
                        .mobileNumber(
                                customer.getMobileNumber())
                        .status(
                                customer.getStatus().name())
                        .createdAt(
                                customer.getCreatedAt())
                        .build();

        customerEventProducer
                .publishCustomerCreatedEvent(event);
    }

    /**
     * CUSTOMER -> RESPONSE
     */
    private CustomerResponse mapToResponse(
            Customer customer) {

        return CustomerResponse.builder()
                .customerId(
                        customer.getCustomerId())
                .cifNumber(
                        customer.getCifNumber())
                .firstName(
                        customer.getFirstName())
                .lastName(
                        customer.getLastName())
                .dateOfBirth(
                        customer.getDateOfBirth())
                .mobileNumber(
                        customer.getMobileNumber())
                .email(
                        customer.getEmail())
                .status(
                        customer.getStatus().name())
                .createdAt(
                        customer.getCreatedAt())
                .updatedAt(
                        customer.getUpdatedAt())
                .build();
    }

    /**
     * ADDRESS -> RESPONSE
     */
    private AddressResponse mapToAddressResponse(
            CustomerAddress address) {

        return AddressResponse.builder()
                .id(address.getId())
                .customerId(
                        address.getCustomerId())
                .addressType(
                        address.getAddressType())
                .addressLine1(
                        address.getAddressLine1())
                .addressLine2(
                        address.getAddressLine2())
                .city(address.getCity())
                .state(address.getState())
                .country(address.getCountry())
                .pinCode(address.getPinCode())
                .createdAt(
                        address.getCreatedAt())
                .updatedAt(
                        address.getUpdatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public List<CustomerResponse> getAllCustomers() {

        return customerRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }


    public CustomerResponse activateCustomer(
            String customerId) {

        System.out.println(
                "ACTIVATE CUSTOMER API CALLED = "
                        + customerId
        );

        Customer customer =
                customerRepository
                        .findByCustomerId(customerId)
                        .orElseThrow(() ->
                                new CustomerNotFoundException(
                                        "Customer not found : "
                                                + customerId));

        customer.setStatus(CustomerStatus.ACTIVE);

        Customer updatedCustomer =
                customerRepository.save(customer);

        CustomerActivatedEvent event =
                CustomerActivatedEvent.builder()
                        .userId(customer.getId())
                        .customerId(customer.getCustomerId())
                        .cifNumber(customer.getCifNumber())
                        .email(customer.getEmail())
                        .firstName(customer.getFirstName())
                        .status(customer.getStatus().name())
                        .build();

        customerKafkaProducer
                .publishCustomerActivated(event);


        System.out.println(
                "CUSTOMER ACTIVATED EVENT SENT");

        return mapToResponse(updatedCustomer);
    }

    public CustomerResponse deactivateCustomer(
            String customerId) {

        Customer customer =
                customerRepository
                        .findByCustomerId(customerId)
                        .orElseThrow(() ->
                                new CustomerNotFoundException(
                                        "Customer not found : "
                                                + customerId));

        customer.setStatus(CustomerStatus.INACTIVE);

        Customer updatedCustomer =
                customerRepository.save(customer);

        return mapToResponse(updatedCustomer);
    }

}