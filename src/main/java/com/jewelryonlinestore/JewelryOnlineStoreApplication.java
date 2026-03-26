package com.jewelryonlinestore;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.jewelryonlinestore.entity.User;
import com.jewelryonlinestore.repository.UserRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootApplication
public class JewelryOnlineStoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(JewelryOnlineStoreApplication.class, args);
    }

    @Bean
    CommandLineRunner initAdmin(UserRepository userRepository,
                                PasswordEncoder passwordEncoder) {
        return args -> {
            String email = "admin@jewelry.com";

            if (userRepository.existsByEmail(email)) {
                log.info("✓ Admin đã tồn tại: {}", email);
                return;
            }

            User admin = User.builder()
                    .email(email)
                    .password(passwordEncoder.encode("Admin@123"))
                    .role(User.Role.ADMIN)
                    .status(User.Status.ACTIVE)
                    .build();

            userRepository.save(admin);
            log.info("✓ Đã tạo admin: {} / Admin@123", email);
        };
    }
}