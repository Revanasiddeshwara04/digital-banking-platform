package com.revana.bank.auth.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;

    private String email;

    private String password;

    private String role;

    @Builder.Default
    private int failedAttempts = 0;

    @Builder.Default
    private boolean accountLocked = false;

    private LocalDateTime lockTime;

    @Column(unique = true)
    private Long accountId;
}