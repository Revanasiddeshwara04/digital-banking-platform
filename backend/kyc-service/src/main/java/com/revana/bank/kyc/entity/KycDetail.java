package com.revana.bank.kyc.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "kyc_details")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KycDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String kycId;

    private String customerId;

    private String cifNumber;

    private String panNumber;

    private String aadhaarNumber;

    @Enumerated(EnumType.STRING)
    private KycStatus status;

    private String remarks;

    private String verifiedBy;

    private LocalDateTime verifiedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {

        createdAt = LocalDateTime.now();

        if (status == null) {
            status = KycStatus.PENDING;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}