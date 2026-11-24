package com.nkhang.lending.ledger;

import com.nkhang.lending.common.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/reconciliation")
public class ReconciliationController {
    private final ReconciliationService service;

    @GetMapping
    public ApiResponse<List<ReconciliationResponse>> accounts() {
        return ApiResponse.success(service.accounts());
    }
}
