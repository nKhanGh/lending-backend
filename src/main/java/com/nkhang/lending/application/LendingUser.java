package com.nkhang.lending.application;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LendingUser {
    @Id
    UUID id;

    @Column(name = "display_name", nullable = false, length = 120)
    String displayName;

    @Column(length = 255)
    String email;

    @Column(name = "password_hash", length = 100)
    String passwordHash;

    @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
    @Column(nullable = false, length = 20)
    UserRole role;

    public static LendingUser customer(String displayName, String email, String passwordHash) {
        LendingUser user = new LendingUser();
        user.id = UUID.randomUUID();
        user.displayName = displayName;
        user.email = email;
        user.passwordHash = passwordHash;
        user.role = UserRole.CUSTOMER;
        return user;
    }

    public static LendingUser admin(String displayName, String email, String passwordHash) {
        LendingUser user = customer(displayName, email, passwordHash);
        user.role = UserRole.ADMIN;
        return user;
    }
}
