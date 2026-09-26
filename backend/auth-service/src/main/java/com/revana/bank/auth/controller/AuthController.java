package com.revana.bank.auth.controller;

import com.revana.bank.auth.dto.*;
import com.revana.bank.auth.jwt.JwtService;
import com.revana.bank.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;


@Tag(
        name = "Authentication APIs",
        description = "Login, Register, JWT Authentication APIs"
)
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;

    public AuthController(
            AuthService authService,
            JwtService jwtService) {

        this.authService = authService;
        this.jwtService = jwtService;
    }

    @GetMapping("/admin/dashboard")
    public String adminDashboard() {

        return "Welcome Admin";
    }

    @GetMapping("/customer/dashboard")
    public String customerDashboard() {

        return "Welcome Customer";
    }


    @GetMapping("/profile")
    public String profile() {

        return "Welcome Authenticated User";
    }

    @Operation(
            summary = "Register New User"
    )
    @PostMapping("/register")
    public String register(
            @Valid
            @RequestBody RegisterRequest request) {

        return authService.register(request);
    }

    @Operation(
            summary = "Login User"
    )
    @PostMapping("/login")
    public AuthResponse login(
            @Valid
            @RequestBody LoginRequest request) {

        return authService.login(request);
    }

    @GetMapping("/validate")
    public String validateToken(
            @RequestHeader("Authorization")
            String authHeader) {

        String token =
                authHeader.substring(7);

        boolean valid =
                jwtService.isTokenValid(token);

        return valid
                ? "Token Valid"
                : "Token Invalid";
    }

    @Operation(
            summary = "Generate New Access Token"
    )
    @PostMapping("/refresh-token")
    public AuthResponse refreshToken(
            @RequestBody
            RefreshTokenRequest request) {

        return authService.refreshToken(
                request);
    }


    @Operation(
            summary = "Logout User"
    )
    @PostMapping("/logout")
    public String logout(
            @RequestBody
            RefreshTokenRequest request) {

        return authService.logout(
                request);
    }
}