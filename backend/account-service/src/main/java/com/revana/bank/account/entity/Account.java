package com.revana.bank.account.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serial;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.io.Serializable;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Account implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;

    private String accountNumber;
    private String customerName;
    private String accountType;
    private BigDecimal balance;
    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime closedAt;
    @PrePersist
    public void prePersist() {

        createdAt = LocalDateTime.now();

        if (balance == null) {
            balance = BigDecimal.ZERO;
        }

        if (status == null) {
            status = "ACTIVE";
        }
    }
}