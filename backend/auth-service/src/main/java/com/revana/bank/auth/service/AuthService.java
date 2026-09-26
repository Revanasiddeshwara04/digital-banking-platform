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


@Service
public class AuthService {

    private final UserRepository repository;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthService(
            UserRepository repository,
            PasswordEncoder encoder,
            JwtService jwtService,
            RefreshTokenService refreshTokenService) {

        this.repository = repository;
        this.encoder = encoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
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

        return "Logged Out Successfully";
    }
}