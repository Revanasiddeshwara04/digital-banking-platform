package com.revana.bank.customer.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerResponse {

    private String customerId;

    private String cifNumber;

    private String firstName;

    private String lastName;

    private LocalDate dateOfBirth;

    private String mobileNumber;

    private String email;

    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}