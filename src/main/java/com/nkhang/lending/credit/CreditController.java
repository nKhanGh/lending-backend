package com.nkhang.lending.credit;

import com.nkhang.lending.common.api.ApiResponse;
import com.nkhang.lending.credit.dto.CreditAccountResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/credit-account")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CreditController {

    CreditService creditService;

    @GetMapping
    public ApiResponse<CreditAccountResponse> getCreditAccount() {
        return ApiResponse.success(creditService.getDemoAccount());
    }
}

