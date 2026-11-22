package com.nkhang.lending.instalment;

import com.nkhang.lending.checkout.Order;
import com.nkhang.lending.common.exception.AppException;
import com.nkhang.lending.common.exception.ErrorCode;
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
    @JoinColumn(name = "order_id", nullable = false)
    Order order;

    @Column(name = "sequence_no", nullable = false)
    short sequenceNo;

    @Column(nullable = false, precision = 18, scale = 2)
    BigDecimal amount;

    @Column(name = "due_date", nullable = false)
    LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    InstalmentStatus status;

    @Column(name = "paid_at")
    Instant paidAt;

    public static Instalment unpaid(Order order, int sequenceNo, BigDecimal amount, LocalDate dueDate) {
        Instalment instalment = new Instalment();
        instalment.id = UUID.randomUUID();
        instalment.order = order;
        instalment.sequenceNo = (short) sequenceNo;
        instalment.amount = amount;
        instalment.dueDate = dueDate;
        instalment.status = InstalmentStatus.UNPAID;
        return instalment;
    }

    public void markPaid(Instant now) {
        if (status == InstalmentStatus.PAID) {
            throw new AppException(ErrorCode.INSTALMENT_ALREADY_PAID);
        }
        status = InstalmentStatus.PAID;
        paidAt = now;
    }
}

