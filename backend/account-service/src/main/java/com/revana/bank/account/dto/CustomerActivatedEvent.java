package com.revana.bank.account.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerActivatedEvent {

    private Long userId;

    private String customerId;

    private String cifNumber;

    private String email;

    private String firstName;

    private String status;
}