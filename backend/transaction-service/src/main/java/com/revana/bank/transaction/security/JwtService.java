package com.revana.bank.transaction.security;

// ════════════════════════════════════════════════════════════════════════════
// ── OLD CODE — no JwtService existed in transaction-service before Feature 3.
//              The class below is new. Original auth-service version kept here
//              for side-by-side reference:
//
// package com.revana.bank.auth.jwt;                 ← different package
//
// @Service
// public class JwtService {
//     @Value("${jwt.secret}")   private String secret;
//     @Value("${jwt.expiration}") private long expiration;
//     private SecretKey key;
//
//     @PostConstruct
//     public void init() { this.key = Keys.hmacShaKeyFor(secret.getBytes()); }
//
//     public String generateToken(String username, String role, Long userId) {
//         return Jwts.builder()
//                 .subject(username)
//                 .claim("role", role)
//                 .claim("userId", userId)
//                 .issuedAt(new Date())
//                 .expiration(new Date(System.currentTimeMillis() + expiration))
//                 .signWith(key)
//                 .compact();
//     }
//
//     public String  extractUsername(String token) { return extractClaims(token).getSubject(); }
//     public String  extractRole(String token)     { return extractClaims(token).get("role", String.class); }
//     public boolean isTokenValid(String token)    { return !extractClaims(token).getExpiration().before(new Date()); }
//
//     private Claims extractClaims(String token) {
//         return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
//     }
// }
// ════════════════════════════════════════════════════════════════════════════

// ── NEW CODE ─────────────────────────────────────────────────────────────────

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * Stateless JWT utility for the transaction-service.
 *
 * This service only READS tokens — it never generates them.
 * Token generation lives exclusively in auth-service.
 *
 * The secret must match the one configured in auth-service so that
 * tokens issued there can be verified here.
 *
 * New vs auth-service: adds extractUserId() so the controller can
 * pass the caller's userId into TransferService without a separate
 * Feign call to auth-service.
 */
@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    private SecretKey key;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
    }

    /** Returns the 'sub' claim (username / email). */
    public String extractUsername(String token) {
        return extractClaims(token).getSubject();
    }

    /** Returns the 'role' custom claim, e.g. "CUSTOMER" or "ADMIN". */
    public String extractRole(String token) {
        return extractClaims(token).get("role", String.class);
    }

    /**
     * Returns the 'userId' custom claim.
     * Used to stamp performedBy on the Transfer record.
     */
    public Long extractUserId(String token) {
        Object userId = extractClaims(token).get("userId");
        if (userId instanceof Integer) return ((Integer) userId).longValue();
        if (userId instanceof Long)    return (Long) userId;
        return null;
    }

    /** Returns true when the token signature is valid and not expired. */
    public boolean isTokenValid(String token) {
        try {
            return !extractClaims(token).getExpiration().before(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    private Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
