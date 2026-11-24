package com.nkhang.lending.application;

import com.nkhang.lending.common.api.ApiResponse;
import com.nkhang.lending.application.dto.CreditApplicationRequest;
import com.nkhang.lending.application.dto.CreditApplicationResponse;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/credit-applications")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CreditApplicationController {
    CreditApplicationService service;

    @PostMapping
    public ResponseEntity<ApiResponse<CreditApplicationResponse>> submit(@Valid @RequestBody CreditApplicationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(service.submit(request), "Credit assessment completed"));
    }

    @GetMapping("/latest")
    public ApiResponse<CreditApplicationResponse> latest() {
        return ApiResponse.success(service.latest());
    }
}
