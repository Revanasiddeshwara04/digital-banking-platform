package com.revana.bank.customer.dto;

import com.revana.bank.customer.entity.AddressType;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressResponse {

    private Long id;

    private Long customerId;

    private AddressType addressType;

    private String addressLine1;

    private String addressLine2;

    private String city;

    private String state;

    private String country;

    private String pinCode;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}