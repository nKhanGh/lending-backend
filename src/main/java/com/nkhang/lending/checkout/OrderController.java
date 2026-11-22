package com.nkhang.lending.checkout;

import com.nkhang.lending.checkout.dto.OrderDetailResponse;
import com.nkhang.lending.checkout.dto.OrderSummaryResponse;
import com.nkhang.lending.common.api.ApiResponse;
import com.nkhang.lending.instalment.dto.InstalmentResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OrderController {

    OrderService orderService;

    @GetMapping
    public ApiResponse<List<OrderSummaryResponse>> getOrders() {
        return ApiResponse.success(orderService.getOrders());
    }

    @GetMapping("/{orderId}")
    public ApiResponse<OrderDetailResponse> getOrder(@PathVariable UUID orderId) {
        return ApiResponse.success(orderService.getOrder(orderId));
    }

    @GetMapping("/{orderId}/instalments")
    public ApiResponse<List<InstalmentResponse>> getInstalments(@PathVariable UUID orderId) {
        return ApiResponse.success(orderService.getInstalments(orderId));
    }
}
