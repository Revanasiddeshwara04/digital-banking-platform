package com.revana.bank.customer.util;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class CIFGenerator {

    private static final String CIF_PREFIX = "CIF";
    private static final String CUSTOMER_PREFIX = "CUST";

    /**
     * Example:
     * CIF20260923123456789
     */
    public String generateCifNumber() {

        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

        int random =
                ThreadLocalRandom.current()
                        .nextInt(100, 999);

        return CIF_PREFIX + timestamp + random;
    }

    /**
     * Example:
     * CUST20260923123456789
     */
    public String generateCustomerId() {

        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

        int random =
                ThreadLocalRandom.current()
                        .nextInt(100, 999);

        return CUSTOMER_PREFIX + timestamp + random;
    }
}