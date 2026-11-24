package com.nkhang.lending.assessment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.math.BigDecimal;

public record CreditProfileRequest(
        @DecimalMin("0.00") @Digits(integer = 17, fraction = 2) BigDecimal verifiedMonthlyIncome,
        @Min(0) @Max(12) int latePayments12m
) { }
