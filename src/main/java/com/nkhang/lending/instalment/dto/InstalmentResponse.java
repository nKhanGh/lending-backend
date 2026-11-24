package com.nkhang.lending.instalment.dto;

import com.nkhang.lending.instalment.Instalment;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record InstalmentResponse(
        UUID id,
        int sequenceNo,
        BigDecimal amount,
        BigDecimal paidAmount,
        BigDecimal remainingAmount,
        LocalDate dueDate,
        String status,
        Instant paidAt
) {
    public static InstalmentResponse from(Instalment instalment) {
        return new InstalmentResponse(instalment.getId(), instalment.getSequenceNo(),
                instalment.getAmount(), instalment.getPaidAmount(), instalment.getRemainingAmount(),
                instalment.getDueDate(), instalment.getStatus().name(),
                instalment.getPaidAt());
    }
}
