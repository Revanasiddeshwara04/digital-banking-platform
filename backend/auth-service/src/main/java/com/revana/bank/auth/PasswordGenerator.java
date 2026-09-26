package com.revana.bank.auth;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordGenerator {

    public static void main(String[] args) {

        BCryptPasswordEncoder encoder =
                new BCryptPasswordEncoder();


        System.out.println("PasswordGenerator");
        System.out.println(
                encoder.encode("admin123"));
    }
}