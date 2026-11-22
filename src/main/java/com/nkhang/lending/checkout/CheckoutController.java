package com.nkhang.lending.checkout;

import com.nkhang.lending.checkout.dto.CheckoutRequest;
import com.nkhang.lending.checkout.dto.CheckoutResponse;
import com.nkhang.lending.checkout.dto.OrderDetailResponse;
import com.nkhang.lending.common.api.ApiResponse;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/checkouts")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CheckoutController {

    CheckoutService checkoutService;

    @PostMapping
    public ResponseEntity<ApiResponse<CheckoutResponse>> createCheckout(
            @Valid @RequestBody CheckoutRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {
        CheckoutResponse response = checkoutService.createCheckout(request, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Credit reserved successfully"));
    }

    @PostMapping("/{checkoutId}/confirm")
    public ApiResponse<OrderDetailResponse> confirmCheckout(
            @PathVariable UUID checkoutId,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {
        return ApiResponse.success(
                checkoutService.confirmCheckout(checkoutId, idempotencyKey),
                "Checkout confirmed successfully");
    }

    @PostMapping("/{checkoutId}/cancel")
    public ApiResponse<OrderDetailResponse> cancelCheckout(@PathVariable UUID checkoutId) {
        return ApiResponse.success(
                checkoutService.cancelCheckout(checkoutId),
                "Checkout cancelled successfully");
    }
}
