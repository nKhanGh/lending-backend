package com.nkhang.lending.drawdown;

import com.nkhang.lending.common.api.ApiResponse;
import com.nkhang.lending.drawdown.dto.DrawdownDetailResponse;
import com.nkhang.lending.drawdown.dto.DrawdownRequest;
import com.nkhang.lending.drawdown.dto.DrawdownResponse;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/credit-drawdowns")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CreditDrawdownController {
    CreditDrawdownService service;

    @PostMapping
    public ResponseEntity<ApiResponse<DrawdownDetailResponse>> create(
            @Valid @RequestBody DrawdownRequest request,
            @RequestHeader("Idempotency-Key") String key) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(service.create(request, key), "Credit utilized successfully"));
    }

    @GetMapping
    public ApiResponse<List<DrawdownResponse>> list() {
        return ApiResponse.success(service.list());
    }

    @GetMapping("/{id}")
    public ApiResponse<DrawdownDetailResponse> detail(@PathVariable UUID id) {
        return ApiResponse.success(service.detail(id));
    }
}
