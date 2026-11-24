package com.nkhang.lending.repayment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SettlePaymentRequest(
        @NotBlank @Pattern(regexp = "SUCCEEDED|FAILED") String outcome,
        @NotBlank @Size(max = 120) String providerEventId
) { }
