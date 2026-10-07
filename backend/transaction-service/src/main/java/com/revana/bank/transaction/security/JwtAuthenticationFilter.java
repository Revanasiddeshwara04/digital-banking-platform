package com.revana.bank.transaction.security;

// ════════════════════════════════════════════════════════════════════════════
// ── OLD CODE — no JWT filter existed in transaction-service before Feature 3.
//              auth-service original for reference:
//
// @Component
// public class JwtAuthenticationFilter extends OncePerRequestFilter {
//     private final JwtService jwtService;
//
//     @Override
//     protected void doFilterInternal(...) {
//         String authHeader = request.getHeader("Authorization");
//         if (authHeader == null || !authHeader.startsWith("Bearer ")) {
//             filterChain.doFilter(request, response); return;
//         }
//         String token = authHeader.substring(7);
//         if (jwtService.isTokenValid(token)) {
//             String username = jwtService.extractUsername(token);
//             String role     = jwtService.extractRole(token);
//             UsernamePasswordAuthenticationToken auth =
//                 new UsernamePasswordAuthenticationToken(
//                     username, null,
//                     List.of(new SimpleGrantedAuthority("ROLE_" + role)));
//             auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
//             SecurityContextHolder.getContext().setAuthentication(auth);
//         }
//         filterChain.doFilter(request, response);
//     }
// }
// ════════════════════════════════════════════════════════════════════════════

// ── NEW CODE ─────────────────────────────────────────────────────────────────

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Stateless JWT authentication filter for the transaction-service.
 *
 * Differences from auth-service version:
 *  1. Also stores userId in a request attribute ("X-UserId") so that
 *     TransferController can pass it to TransferService without a
 *     second Feign call.
 *  2. Uses transaction-service's own JwtService (same package).
 *  3. Logs at DEBUG instead of System.out.println.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        // No bearer token — pass through; SecurityConfig will reject if endpoint requires auth
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        if (jwtService.isTokenValid(token)) {

            String username = jwtService.extractUsername(token);
            String role     = jwtService.extractRole(token);
            Long   userId   = jwtService.extractUserId(token);

            log.debug("JWT valid — username={} role={} userId={}", username, role, userId);

            // Store userId as a request attribute for controllers to read
            if (userId != null) {
                request.setAttribute("X-UserId", userId);
            }
            request.setAttribute("X-Username", username);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            username,
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + role)));

            authentication.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authentication);

        } else {
            log.warn("Invalid or expired JWT token received");
        }

        filterChain.doFilter(request, response);
    }
}
