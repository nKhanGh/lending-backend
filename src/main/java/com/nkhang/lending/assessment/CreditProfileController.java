package com.nkhang.lending.assessment;

import com.nkhang.lending.assessment.dto.CreditProfileRequest;
import com.nkhang.lending.assessment.dto.CreditProfileResponse;
import com.nkhang.lending.common.api.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/credit-profiles")
public class CreditProfileController {
    private final CreditProfileService service;

    @PutMapping("/{userId}")
    public ApiResponse<CreditProfileResponse> upsert(@PathVariable UUID userId,
            @Valid @RequestBody CreditProfileRequest request) {
        return ApiResponse.success(service.upsert(userId, request));
    }
}
