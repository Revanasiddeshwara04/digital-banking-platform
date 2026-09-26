package com.revana.bank.apigateway.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    private SecretKey key;

    @PostConstruct
    public void init() {

        key = Keys.hmacShaKeyFor(
                secret.getBytes());
    }

    public boolean isTokenValid(String token) {

        try {

            extractClaims(token);

            System.out.println("JWT VALID");

            return true;

        } catch (Exception e) {

            System.out.println("JWT ERROR: " + e.getMessage());

            return false;
        }
    }

    public String extractRole(String token) {

        return extractClaims(token)
                .get("role", String.class);
    }

    public String extractUsername(String token) {

        return extractClaims(token)
                .getSubject();
    }
    public Long extractUserId(
            String token) {

        Integer userId =
                extractClaims(token)
                        .get("userId",
                                Integer.class);

        return userId.longValue();
    }

    private Claims extractClaims(String token) {

        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}