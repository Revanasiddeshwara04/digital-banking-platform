package com.revana.bank.auth.config;


import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class DatabaseHealthCheck {

    @Bean
    CommandLineRunner checkDb(DataSource dataSource) {
        return args -> {
            try {
                dataSource.getConnection();
                System.out.println("==================================");
                System.out.println("✅ Database connection successful");
                System.out.println("==================================");
            } catch (Exception e) {
                throw new RuntimeException(
                        "❌ MySQL is not running or not reachable!", e);
            }
        };
    }
}
