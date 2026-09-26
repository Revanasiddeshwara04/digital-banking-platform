package com.revana.bank.auth.service;

import com.revana.bank.auth.dto.*;
import com.revana.bank.auth.entity.User;
import com.revana.bank.auth.exception.UserNotFoundException;
import com.revana.bank.auth.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository repository;
    private final PasswordEncoder encoder;

    public UserService(
            UserRepository repository,
            PasswordEncoder encoder) {

        this.repository = repository;
        this.encoder = encoder;
    }

    public UserProfileResponse getProfile(
            String username) {

        User user = repository
                .findByUsername(username)
                .orElseThrow(() ->
                        new UserNotFoundException(
                                "User Not Found"));

        return UserProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }

    public String updateProfile(
            String username,
            UpdateProfileRequest request) {

        User user = repository
                .findByUsername(username)
                .orElseThrow(() ->
                        new UserNotFoundException(
                                "User Not Found"));

        user.setEmail(request.getEmail());

        repository.save(user);

        return "Profile Updated Successfully";
    }

    public String changePassword(
            String username,
            ChangePasswordRequest request) {

        User user = repository
                .findByUsername(username)
                .orElseThrow(() ->
                        new UserNotFoundException(
                                "User Not Found"));

        if (!encoder.matches(
                request.getOldPassword(),
                user.getPassword())) {

            throw new RuntimeException(
                    "Old Password Incorrect");
        }

        user.setPassword(
                encoder.encode(
                        request.getNewPassword()));

        repository.save(user);

        return "Password Changed Successfully";
    }
}