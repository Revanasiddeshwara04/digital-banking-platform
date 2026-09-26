package com.revana.bank.customer.kafka;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CustomerActivatedEvent {

    private Long userId;
    private String customerId;
    private String cifNumber;
    private String email;
    private String firstName;
    private String status;
}
