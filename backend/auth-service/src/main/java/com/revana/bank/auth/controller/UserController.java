package com.revana.bank.auth.controller;

import com.revana.bank.auth.dto.*;
import com.revana.bank.auth.jwt.JwtService;
import com.revana.bank.auth.service.UserService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final JwtService jwtService;

    public UserController(
            UserService userService,
            JwtService jwtService) {

        this.userService = userService;
        this.jwtService = jwtService;
    }

    @GetMapping("/me")
    public UserProfileResponse getProfile(
            @RequestHeader("Authorization")
            String authHeader) {

        String token =
                authHeader.substring(7);

        String username =
                jwtService.extractUsername(token);

        return userService.getProfile(
                username);
    }

    @PutMapping("/profile")
    public String updateProfile(
            @RequestHeader("Authorization")
            String authHeader,
            @RequestBody
            UpdateProfileRequest request) {

        System.out.println("UPDATE PROFILE API HIT");

        String username =
                jwtService.extractUsername(
                        authHeader.substring(7));

        return userService.updateProfile(
                username,
                request);
    }

    @PutMapping("/change-password")
    public String changePassword(
            @RequestHeader("Authorization")
            String authHeader,
            @RequestBody
            ChangePasswordRequest request) {

        String username =
                jwtService.extractUsername(
                        authHeader.substring(7));

        return userService.changePassword(
                username,
                request);
    }
}