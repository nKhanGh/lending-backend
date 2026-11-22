package com.nkhang.lending.checkout;

import com.nkhang.lending.checkout.dto.OrderDetailResponse;
import com.nkhang.lending.checkout.dto.OrderSummaryResponse;
import com.nkhang.lending.common.exception.AppException;
import com.nkhang.lending.common.exception.ErrorCode;
import com.nkhang.lending.instalment.Instalment;
import com.nkhang.lending.instalment.InstalmentRepository;
import com.nkhang.lending.instalment.dto.InstalmentResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderService {

    final OrderRepository orderRepository;
    final CreditReservationRepository reservationRepository;
    final InstalmentRepository instalmentRepository;

    @Value("${lending.demo-user-id}")
    UUID demoUserId;

    @Transactional(readOnly = true)
    public List<OrderSummaryResponse> getOrders() {
        return orderRepository.findAllByUserIdOrderByCreatedAtDesc(demoUserId).stream()
                .map(OrderSummaryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderDetailResponse getOrder(UUID orderId) {
        Order order = findOwnedOrder(orderId);
        return buildDetail(order);
    }

    @Transactional(readOnly = true)
    public List<InstalmentResponse> getInstalments(UUID orderId) {
        findOwnedOrder(orderId);
        return instalmentRepository.findAllByOrderIdOrderBySequenceNoAsc(orderId).stream()
                .map(InstalmentResponse::from)
                .toList();
    }

    OrderDetailResponse buildDetail(Order order) {
        CreditReservation reservation = reservationRepository.findByOrderId(order.getId())
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Reservation not found"));
        List<Instalment> instalments = instalmentRepository.findAllByOrderIdOrderBySequenceNoAsc(order.getId());
        return OrderDetailResponse.from(order, reservation, instalments);
    }

    private Order findOwnedOrder(UUID orderId) {
        return orderRepository.findDetailByIdAndUserId(orderId, demoUserId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Order not found"));
    }
}

