package com.revana.bank.account.feign;

import com.revana.bank.account.dto.KycResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "kyc-service")
public interface KycClient {

    @GetMapping("/api/kyc/{kycId}")
    KycResponse getKyc(
            @PathVariable("kycId") String kycId);
}