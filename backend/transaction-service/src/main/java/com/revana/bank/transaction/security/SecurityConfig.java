package com.revana.bank.transaction.security;

// ════════════════════════════════════════════════════════════════════════════
// ── OLD CODE — no SecurityConfig existed in transaction-service before F3.
//              auth-service original for reference:
//
// @Configuration
// public class SecurityConfig {
//     @Bean
//     public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
//         http.csrf(csrf -> csrf.disable())
//             .sessionManagement(s -> s.sessionCreationPolicy(STATELESS))
//             .authorizeHttpRequests(auth -> auth
//                 .requestMatchers("/api/auth/register","/api/auth/login",...).permitAll()
//                 .requestMatchers("/api/auth/admin/**").hasRole("ADMIN")
//                 .requestMatchers("/api/auth/customer/**").hasRole("CUSTOMER")
//                 .anyRequest().authenticated())
//             .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
//             .httpBasic(b -> b.disable());
//         return http.build();
//     }
// }
// ════════════════════════════════════════════════════════════════════════════

// ── NEW CODE ─────────────────────────────────────────────────────────────────

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security rules for the transaction-service.
 *
 * Role matrix (from requirements):
 *   POST /api/transfers          → CUSTOMER only  (initiate transfer)
 *   GET  /api/transfers/**       → CUSTOMER + ADMIN (view own / all)
 *   GET  /api/transactions/**    → CUSTOMER + ADMIN (legacy endpoints)
 *   /v3/api-docs/**, /swagger-ui/** → public (developer tooling)
 *   actuator/health              → public (liveness probe)
 */
@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
            // Stateless REST API — no CSRF protection needed
            .csrf(AbstractHttpConfigurer::disable)

            // No HTTP session; every request must carry a valid JWT
            .sessionManagement(session ->
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            .authorizeHttpRequests(auth -> auth

                // ── Public endpoints ─────────────────────────────────────
                .requestMatchers(
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/actuator/health",
                        "/actuator/info"
                ).permitAll()

                // ── Transfer endpoints ────────────────────────────────────
                // Only CUSTOMER may initiate a transfer
                .requestMatchers(HttpMethod.POST, "/api/transfers")
                        .hasRole("CUSTOMER")

                // CUSTOMER can view own history; ADMIN can view all
                .requestMatchers(HttpMethod.GET, "/api/transfers/**")
                        .hasAnyRole("CUSTOMER", "ADMIN")

                // ── Legacy transaction endpoints ──────────────────────────
                .requestMatchers("/api/transactions/**")
                        .hasAnyRole("CUSTOMER", "ADMIN")

                // ── Everything else requires authentication ───────────────
                .anyRequest().authenticated()
            )

            // Register JWT filter before Spring's username/password filter
            .addFilterBefore(jwtAuthenticationFilter,
                    UsernamePasswordAuthenticationFilter.class)

            // Disable HTTP Basic (not used in this service)
            .httpBasic(AbstractHttpConfigurer::disable);

        return http.build();
    }
}
