package com.revana.bank.kyc.repository;

import com.revana.bank.kyc.entity.KycDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface KycRepository
        extends JpaRepository<KycDetail, Long> {

    Optional<KycDetail> findByKycId(
            String kycId);

    Optional<KycDetail> findByCustomerId(
            String customerId);
}