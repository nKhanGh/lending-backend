package com.nkhang.lending.audit;

import com.nkhang.lending.common.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/audit-events")
public class AuditController {
    private final AuditService service;

    @GetMapping
    public ApiResponse<List<AuditEventResponse>> recent(
            @RequestParam(required = false) UUID userId,
            @RequestParam(defaultValue = "50") int limit) {
        return ApiResponse.success(service.recent(userId, limit));
    }
}
