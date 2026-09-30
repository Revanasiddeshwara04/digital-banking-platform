package com.revana.bank.auth.service;

import com.revana.bank.auth.dto.*;
import com.revana.bank.auth.entity.RefreshToken;
import com.revana.bank.auth.entity.User;
import com.revana.bank.auth.exception.InvalidCredentialsException;
import com.revana.bank.auth.exception.UserAlreadyExistsException;
import com.revana.bank.auth.jwt.JwtService;
import com.revana.bank.auth.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.revana.bank.auth.exception.AccountLockedException;
import java.time.LocalDateTime;
import com.revana.bank.auth.dto.AuditEvent;
import com.revana.bank.auth.kafka.AuditProducer;

import java.util.UUID;


@Service
public class AuthService {

    private final UserRepository repository;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final AuditProducer auditProducer;

    public AuthService(
            UserRepository repository,
            PasswordEncoder encoder,
            JwtService jwtService,
            RefreshTokenService refreshTokenService, AuditProducer auditProducer) {

        this.repository = repository;
        this.encoder = encoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.auditProducer = auditProducer;
    }

    public String register(RegisterRequest request) {

        if (repository.existsByUsername(
                request.getUsername())) {

            throw new UserAlreadyExistsException(
                    "User is already registered");
        }

        if (repository.existsByEmail(
                request.getEmail())) {

            throw new UserAlreadyExistsException(
                    "Email is already registered");
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(
                        encoder.encode(
                                request.getPassword()))
                .role("CUSTOMER")
                .build();

        repository.save(user);

        auditProducer.publish(

                AuditEvent.builder()
                        .auditId(UUID.randomUUID().toString())
                        .eventType("USER_REGISTERED")
                        .serviceName("AUTH-SERVICE")
                        .entityId(user.getId() != null ?
                                user.getId().toString() :
                                user.getUsername())
                        .performedBy(user.getUsername())
                        .actionStatus("SUCCESS")
                        .eventTimestamp(LocalDateTime.now())
                        .payload(user.getEmail())
                        .build()
        );

        return "User Registered Successfully";
    }
    public AuthResponse login(
            LoginRequest request) {

        User user =
                repository.findByUsername(
                                request.getUsername())
                        .orElseThrow(() ->
                                new InvalidCredentialsException(
                                        "Invalid Username or Password"));
        if (user.isAccountLocked()) {

            if (user.getLockTime() != null &&
                    user.getLockTime()
                            .plusMinutes(15)
                            .isBefore(LocalDateTime.now())) {

                user.setAccountLocked(false);
                user.setFailedAttempts(0);
                user.setLockTime(null);

                repository.save(user);

            } else {

                throw new AccountLockedException(
                        "Account is locked. Try again after 15 minutes.");
            }
        }
        if (!encoder.matches(
                request.getPassword(),
                user.getPassword())) {

            user.setFailedAttempts(
                    user.getFailedAttempts() + 1);

            if (user.getFailedAttempts() >= 5) {

                user.setAccountLocked(true);
                user.setLockTime(
                        LocalDateTime.now());

                repository.save(user);

                auditProducer.publish(

                        AuditEvent.builder()
                                .auditId(UUID.randomUUID().toString())
                                .eventType("LOGIN_FAILED")
                                .serviceName("AUTH-SERVICE")
                                .entityId(user.getId().toString())
                                .performedBy(user.getUsername())
                                .actionStatus("FAILED")
                                .eventTimestamp(LocalDateTime.now())
                                .payload("Invalid password")
                                .build()
                );

                throw new AccountLockedException(
                        "Account locked due to multiple failed login attempts.");
            }

            repository.save(user);

            throw new InvalidCredentialsException(
                    "Invalid Username or Password");
        }

        user.setFailedAttempts(0);
        repository.save(user);

        String accessToken =
                jwtService.generateToken(
                        user.getUsername(),
                        user.getRole(),
                        user.getId());

        RefreshToken refreshToken =
                refreshTokenService
                        .createRefreshToken(user);

        auditProducer.publish(

                AuditEvent.builder()
                        .auditId(UUID.randomUUID().toString())
                        .eventType("LOGIN_SUCCESS")
                        .serviceName("AUTH-SERVICE")
                        .entityId(user.getId().toString())
                        .performedBy(user.getUsername())
                        .actionStatus("SUCCESS")
                        .eventTimestamp(LocalDateTime.now())
                        .payload(user.getEmail())
                        .build()
        );

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(
                        refreshToken.getToken())
                .username(user.getUsername())
                .role(user.getRole())
                .build();
    }

    public AuthResponse refreshToken(
            RefreshTokenRequest request) {

        RefreshToken refreshToken =
                refreshTokenService.validateToken(
                        request.getRefreshToken());

        User user = refreshToken.getUser();

        String accessToken =
                jwtService.generateToken(
                        user.getUsername(),
                        user.getRole(),
        user.getId());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(
                        refreshToken.getToken())
                .username(user.getUsername())
                .role(user.getRole())
                .build();
    }

    public String logout(
            RefreshTokenRequest request) {

        refreshTokenService.validateToken(
                request.getRefreshToken());

        refreshTokenService.deleteToken(
                request.getRefreshToken());

        auditProducer.publish(

                AuditEvent.builder()
                        .auditId(UUID.randomUUID().toString())
                        .eventType("LOGOUT")
                        .serviceName("AUTH-SERVICE")
                        .entityId("NA")
                        .performedBy("USER")
                        .actionStatus("SUCCESS")
                        .eventTimestamp(LocalDateTime.now())
                        .payload("User logged out")
                        .build()
        );

        return "Logged Out Successfully";
    }
}