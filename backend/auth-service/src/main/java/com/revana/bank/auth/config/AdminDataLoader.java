package com.revana.bank.auth.config;

import com.revana.bank.auth.entity.User;
import com.revana.bank.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminDataLoader implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {

        if (!userRepository.existsByUsername("admin")) {

            User admin = User.builder()
                    .username("admin")
                    .email("admin@bank.com")
                    .password(passwordEncoder.encode("Admin@123"))
                    .role("ADMIN")
                    .build();

            userRepository.save(admin);

            System.out.println("ADMIN USER CREATED");
        }
    }
}