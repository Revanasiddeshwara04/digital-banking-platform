package com.revana.bank.kyc.service;

import com.revana.bank.kyc.client.CustomerFeignClient;
import com.revana.bank.kyc.dto.AuditEvent;
import com.revana.bank.kyc.dto.CreateKycRequest;
import com.revana.bank.kyc.dto.KycResponse;
import com.revana.bank.kyc.entity.KycDetail;
import com.revana.bank.kyc.entity.KycStatus;
import com.revana.bank.kyc.kafka.AuditProducer;
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
    private final AuditProducer auditProducer;

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

        KycDetail savedKyc =
                kycRepository.save(kyc);

        auditProducer.publish(
                AuditEvent.builder()
                        .auditId(java.util.UUID.randomUUID().toString())
                        .eventType("KYC_CREATED")
                        .serviceName("KYC-SERVICE")
                        .entityId(savedKyc.getKycId())
                        .performedBy(savedKyc.getCustomerId())
                        .actionStatus("SUCCESS")
                        .eventTimestamp(LocalDateTime.now())
                        .payload(savedKyc.getPanNumber())
                        .build()
        );

        return mapToResponse(savedKyc);
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

        System.out.println("After Feign Call");

        kyc.setStatus(KycStatus.APPROVED);
        kyc.setVerifiedBy(verifiedBy);
        kyc.setVerifiedAt(LocalDateTime.now());

        KycDetail savedKyc =
                kycRepository.save(kyc);

        auditProducer.publish(
                AuditEvent.builder()
                        .auditId(java.util.UUID.randomUUID().toString())
                        .eventType("KYC_APPROVED")
                        .serviceName("KYC-SERVICE")
                        .entityId(savedKyc.getKycId())
                        .performedBy(verifiedBy)
                        .actionStatus("SUCCESS")
                        .eventTimestamp(LocalDateTime.now())
                        .payload(savedKyc.getCustomerId())
                        .build()
        );

        return mapToResponse(savedKyc);
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

        KycDetail savedKyc =
                kycRepository.save(kyc);

        auditProducer.publish(
                AuditEvent.builder()
                        .auditId(java.util.UUID.randomUUID().toString())
                        .eventType("KYC_REJECTED")
                        .serviceName("KYC-SERVICE")
                        .entityId(savedKyc.getKycId())
                        .performedBy("ADMIN")
                        .actionStatus("SUCCESS")
                        .eventTimestamp(LocalDateTime.now())
                        .payload(remarks)
                        .build()
        );

        return mapToResponse(savedKyc);
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