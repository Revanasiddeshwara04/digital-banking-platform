package com.revana.bank.customer.kafka.event;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class CustomerCreatedEvent {

    private String customerId;

    private String cifNumber;

    private String firstName;

    private String lastName;

    private String email;

    private String mobileNumber;

    private String status;

    private LocalDateTime createdAt;
}