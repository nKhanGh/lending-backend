package com.nkhang.lending.repayment;

import com.nkhang.lending.common.api.ApiResponse;
import com.nkhang.lending.repayment.dto.PaymentAttemptResponse;
import com.nkhang.lending.repayment.dto.RepaymentHistoryResponse;
import com.nkhang.lending.repayment.dto.RepaymentRequest;
import com.nkhang.lending.repayment.dto.SettlePaymentRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
@RequestMapping("/api")
public class RepaymentController {
    private final RepaymentService service;

    @PostMapping("/instalments/{id}/repay")
    public ResponseEntity<ApiResponse<PaymentAttemptResponse>> request(@PathVariable UUID id,
            @RequestHeader("Idempotency-Key") String key, @Valid @RequestBody(required = false) RepaymentRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(service.request(id, key, request == null ? null : request.amount()), "Payment pending"));
    }

    @GetMapping("/payment-attempts/{id}")
    public ApiResponse<PaymentAttemptResponse> attempt(@PathVariable UUID id) {
        return ApiResponse.success(service.attempt(id));
    }

    @GetMapping("/credit-drawdowns/{id}/payment-attempts")
    public ApiResponse<List<PaymentAttemptResponse>> attempts(@PathVariable UUID id) {
        return ApiResponse.success(service.attempts(id));
    }

    @GetMapping("/credit-drawdowns/{id}/repayments")
    public ApiResponse<List<RepaymentHistoryResponse>> history(@PathVariable UUID id) {
        return ApiResponse.success(service.history(id));
    }

    @PostMapping("/admin/payment-attempts/{id}/settle")
    public ApiResponse<PaymentAttemptResponse> settle(@PathVariable UUID id,
            @Valid @RequestBody SettlePaymentRequest request) {
        return ApiResponse.success(service.settle(id, PaymentAttemptStatus.valueOf(request.outcome()),
                request.providerEventId()));
    }
}
