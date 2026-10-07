package com.revana.bank.transaction;

// ════════════════════════════════════════════════════════════════════════════
// ── OLD CODE (original — no Feign, no OpenAPI config)
//
// @SpringBootApplication
// public class TransactionServiceApplication {
//     public static void main(String[] args) {
//         SpringApplication.run(TransactionServiceApplication.class, args);
//     }
// }
// ════════════════════════════════════════════════════════════════════════════

// ── NEW CODE ─────────────────────────────────────────────────────────────────

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Transaction Service bootstrap.
 *
 * Changes in Feature 3:
 *  - @EnableFeignClients activates AccountClient for service discovery via Eureka.
 *  - @OpenAPIDefinition adds global Swagger metadata.
 *  - @SecurityScheme registers the JWT bearer scheme in Swagger UI so all
 *    endpoints show the "Authorize" button automatically.
 */
@SpringBootApplication
@EnableFeignClients(basePackages = "com.revana.bank.transaction.client")
@OpenAPIDefinition(
        info = @Info(
                title       = "Transaction Service API",
                version     = "1.0",
                description = "Fund transfer and transaction management for Revana Digital Banking"
        )
)
@SecurityScheme(
        name   = "bearerAuth",
        type   = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        in     = SecuritySchemeIn.HEADER
)
public class TransactionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TransactionServiceApplication.class, args);
    }
}
