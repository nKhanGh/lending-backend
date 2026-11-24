package com.nkhang.lending.auth;

import com.nkhang.lending.application.LendingUserRepository;
import com.nkhang.lending.auth.dto.UserResponse;
import com.nkhang.lending.common.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminController {
    private final LendingUserRepository users;

    @GetMapping("/users")
    public ApiResponse<List<UserResponse>> users() {
        return ApiResponse.success(users.findAll().stream().map(UserResponse::from).toList());
    }
}
