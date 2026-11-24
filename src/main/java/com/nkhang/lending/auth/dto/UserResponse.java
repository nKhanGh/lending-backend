package com.nkhang.lending.auth.dto;

import com.nkhang.lending.application.LendingUser;
import java.util.UUID;

public record UserResponse(UUID userId, String displayName, String email, String role) {
    public static UserResponse from(LendingUser user) {
        return new UserResponse(user.getId(), user.getDisplayName(), user.getEmail(), user.getRole().name());
    }
}
