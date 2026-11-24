package com.nkhang.lending.instalment.dto;

import com.nkhang.lending.instalment.Instalment;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public record InstalmentResponse(
        UUID id,
        int sequenceNo,
        BigDecimal amount,
        BigDecimal paidAmount,
        BigDecimal remainingAmount,
        LocalDate dueDate,
        String status,
        long daysOverdue,
        Instant paidAt
) {
    public static InstalmentResponse from(Instalment instalment, LocalDate today) {
        boolean overdue = instalment.getStatus() != com.nkhang.lending.instalment.InstalmentStatus.PAID
                && instalment.getDueDate().isBefore(today);
        return new InstalmentResponse(instalment.getId(), instalment.getSequenceNo(),
                instalment.getAmount(), instalment.getPaidAmount(), instalment.getRemainingAmount(),
                instalment.getDueDate(), overdue ? "OVERDUE" : instalment.getStatus().name(),
                overdue ? ChronoUnit.DAYS.between(instalment.getDueDate(), today) : 0,
                instalment.getPaidAt());
    }
}
