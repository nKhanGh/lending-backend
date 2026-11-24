package com.nkhang.lending.instalment;

import com.nkhang.lending.drawdown.CreditDrawdown;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Entity
@Table(name = "instalments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Instalment {
    @Id
    UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "drawdown_id", nullable = false)
    CreditDrawdown drawdown;

    @Column(name = "sequence_no", nullable = false)
    int sequenceNo;

    @Column(nullable = false, precision = 19, scale = 2)
    BigDecimal amount;

    @Column(name = "due_date", nullable = false)
    LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    InstalmentStatus status;

    @Column(name = "paid_at")
    Instant paidAt;

    public static Instalment unpaid(CreditDrawdown drawdown, int sequenceNo,
                                   BigDecimal amount, LocalDate dueDate) {
        Instalment instalment = new Instalment();
        instalment.id = UUID.randomUUID();
        instalment.drawdown = drawdown;
        instalment.sequenceNo = sequenceNo;
        instalment.amount = amount;
        instalment.dueDate = dueDate;
        instalment.status = InstalmentStatus.UNPAID;
        return instalment;
    }

    public void markPaid(Instant now) {
        if (status != InstalmentStatus.UNPAID) {
            throw new IllegalStateException("Instalment is already paid");
        }
        status = InstalmentStatus.PAID;
        paidAt = now;
    }
}
