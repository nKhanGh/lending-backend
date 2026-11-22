package com.nkhang.lending.repayment;

import com.nkhang.lending.common.api.ApiResponse;
import com.nkhang.lending.repayment.dto.RepaymentHistoryResponse;
import com.nkhang.lending.repayment.dto.RepaymentResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RepaymentController {

    RepaymentService repaymentService;

    @PostMapping("/instalments/{instalmentId}/pay")
    public ApiResponse<RepaymentResponse> payInstalment(
            @PathVariable UUID instalmentId,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {
        return ApiResponse.success(
                repaymentService.pay(instalmentId, idempotencyKey),
                "Instalment paid successfully");
    }

    @GetMapping("/repayments")
    public ApiResponse<List<RepaymentHistoryResponse>> getRepayments() {
        return ApiResponse.success(repaymentService.getHistory());
    }
}
