package com.revana.bank.auth.service;

import com.revana.bank.auth.entity.RefreshToken;
import com.revana.bank.auth.entity.User;
import com.revana.bank.auth.exception.InvalidCredentialsException;
import com.revana.bank.auth.exception.RefreshTokenException;
import com.revana.bank.auth.repository.RefreshTokenRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository repository;

    public RefreshTokenService(
            RefreshTokenRepository repository) {

        this.repository = repository;
    }

    public RefreshToken createRefreshToken(
            User user) {

        RefreshToken refreshToken =
                RefreshToken.builder()
                        .token(UUID.randomUUID().toString())
                        .expiryDate(
                                LocalDateTime.now()
                                        .plusDays(7))
                        .user(user)
                        .build();

        return repository.save(refreshToken);
    }

    public RefreshToken validateToken(
            String token) {

        RefreshToken refreshToken =
                repository.findByToken(token)
                        .orElseThrow(() ->
                                new RefreshTokenException(
                                        "Invalid Refresh Token"));

        if (refreshToken.getExpiryDate()
                .isBefore(LocalDateTime.now())) {

            throw new InvalidCredentialsException(
                    "Invalid Refresh Token");
        }

        return refreshToken;
    }

    public void deleteToken(
            String token) {

        repository.deleteByToken(token);
    }
}