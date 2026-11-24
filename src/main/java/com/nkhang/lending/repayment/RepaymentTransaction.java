package com.nkhang.lending.repayment;

import com.nkhang.lending.instalment.Instalment;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@Table(name = "repayment_transactions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RepaymentTransaction {
    @Id
    UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instalment_id", nullable = false, unique = true)
    Instalment instalment;

    @Column(nullable = false, precision = 19, scale = 2)
    BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    RepaymentStatus status;

    @Column(name = "paid_at", nullable = false)
    Instant paidAt;

    public static RepaymentTransaction success(Instalment instalment, Instant now) {
        RepaymentTransaction repayment = new RepaymentTransaction();
        repayment.id = UUID.randomUUID();
        repayment.instalment = instalment;
        repayment.amount = instalment.getAmount();
        repayment.status = RepaymentStatus.SUCCESS;
        repayment.paidAt = now;
        return repayment;
    }
}
