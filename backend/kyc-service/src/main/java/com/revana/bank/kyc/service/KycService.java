package com.revana.bank.kyc.service;

import com.revana.bank.kyc.client.CustomerFeignClient;
import com.revana.bank.kyc.dto.CreateKycRequest;
import com.revana.bank.kyc.dto.KycResponse;
import com.revana.bank.kyc.entity.KycDetail;
import com.revana.bank.kyc.entity.KycStatus;
import com.revana.bank.kyc.repository.KycRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class KycService {

    private final KycRepository kycRepository;
    private final CustomerFeignClient customerFeignClient;

    public KycResponse createKyc(
            CreateKycRequest request) {

        KycDetail kyc = KycDetail.builder()
                .kycId("KYC" + System.currentTimeMillis())
                .customerId(request.getCustomerId())
                .cifNumber(request.getCifNumber())
                .panNumber(request.getPanNumber())
                .aadhaarNumber(request.getAadhaarNumber())
                .status(KycStatus.PENDING)
                .build();

        return mapToResponse(
                kycRepository.save(kyc));
    }

    @Transactional(readOnly = true)
    public KycResponse getKyc(
            String kycId) {

        KycDetail kyc =
                kycRepository.findByKycId(kycId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "KYC not found"));

        return mapToResponse(kyc);
    }

    public KycResponse approveKyc(
            String kycId,
            String verifiedBy) {

        KycDetail kyc =
                kycRepository.findByKycId(kycId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "KYC not found"));

        System.out.println(
                "Before Feign Call");

        customerFeignClient
                .activateCustomer(
                        kyc.getCustomerId());

        System.out.println(
                "After Feign Call");

        kyc.setStatus(KycStatus.APPROVED);
        kyc.setVerifiedBy(verifiedBy);
        kyc.setVerifiedAt(LocalDateTime.now());

        return mapToResponse(
                kycRepository.save(kyc));
    }

    public KycResponse rejectKyc(
            String kycId,
            String remarks) {

        KycDetail kyc =
                kycRepository.findByKycId(kycId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "KYC not found"));

        kyc.setStatus(KycStatus.REJECTED);
        kyc.setRemarks(remarks);

        return mapToResponse(
                kycRepository.save(kyc));
    }

    private KycResponse mapToResponse(
            KycDetail kyc) {

        return KycResponse.builder()
                .kycId(kyc.getKycId())
                .customerId(kyc.getCustomerId())
                .cifNumber(kyc.getCifNumber())
                .panNumber(kyc.getPanNumber())
                .aadhaarNumber(kyc.getAadhaarNumber())
                .status(kyc.getStatus().name())
                .remarks(kyc.getRemarks())
                .verifiedBy(kyc.getVerifiedBy())
                .verifiedAt(kyc.getVerifiedAt())
                .createdAt(kyc.getCreatedAt())
                .updatedAt(kyc.getUpdatedAt())
                .build();
    }
}