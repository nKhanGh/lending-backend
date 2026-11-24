package com.nkhang.lending.config;

import com.nkhang.lending.application.LendingUser;
import com.nkhang.lending.application.LendingUserRepository;
import com.nkhang.lending.application.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
@RequiredArgsConstructor
public class AdminBootstrap implements ApplicationRunner {
    private final LendingUserRepository users;
    private final PasswordEncoder passwordEncoder;

    @Value("${lending.bootstrap-admin-email:}")
    private String email;
    @Value("${lending.bootstrap-admin-password:}")
    private String password;

    @Override
    public void run(ApplicationArguments args) {
        if (email.isBlank() && password.isBlank()) return;
        if (email.isBlank() || password.length() < 12) {
            throw new IllegalStateException("Set BOOTSTRAP_ADMIN_EMAIL and a password of at least 12 characters");
        }
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        users.findByEmailIgnoreCase(normalized).ifPresentOrElse(existing -> {
            if (existing.getRole() != UserRole.ADMIN) {
                throw new IllegalStateException("BOOTSTRAP_ADMIN_EMAIL belongs to a customer");
            }
        }, () -> users.save(LendingUser.admin("Administrator", normalized, passwordEncoder.encode(password))));
    }
}
