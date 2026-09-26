package com.revana.bank.account.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RateLimiterService {

    private final Bucket depositBucket;
    private final Bucket withdrawBucket;
    private final Bucket transferBucket;

    public RateLimiterService() {

        depositBucket = Bucket.builder()
                .addLimit(
                        Bandwidth.builder()
                                .capacity(5)
                                .refillGreedy(
                                        5,
                                        Duration.ofMinutes(1))
                                .build())
                .build();

        withdrawBucket = Bucket.builder()
                .addLimit(
                        Bandwidth.builder()
                                .capacity(5)
                                .refillGreedy(
                                        5,
                                        Duration.ofMinutes(1))
                                .build())
                .build();

        transferBucket = Bucket.builder()
                .addLimit(
                        Bandwidth.builder()
                                .capacity(3)
                                .refillGreedy(
                                        3,
                                        Duration.ofMinutes(1))
                                .build())
                .build();
    }

    public boolean allowDeposit() {
        return depositBucket.tryConsume(1);
    }

    public boolean allowWithdraw() {
        return withdrawBucket.tryConsume(1);
    }

    public boolean allowTransfer() {
        return transferBucket.tryConsume(1);
    }
}