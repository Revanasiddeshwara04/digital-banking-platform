package com.revana.bank.auth.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateAccountRequest {

    private String customerName;

    private String accountType;

    private BigDecimal balance;
}