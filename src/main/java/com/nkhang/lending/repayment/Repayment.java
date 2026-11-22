package com.nkhang.lending.repayment;

import com.nkhang.lending.instalment.Instalment;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "repayments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Repayment {

    @Id
    UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instalment_id", nullable = false, unique = true)
    Instalment instalment;

    @Column(nullable = false, precision = 18, scale = 2)
    BigDecimal amount;

    @Column(name = "created_at", nullable = false)
    Instant createdAt;

    public static Repayment create(Instalment instalment, Instant now) {
        Repayment repayment = new Repayment();
        repayment.id = UUID.randomUUID();
        repayment.instalment = instalment;
        repayment.amount = instalment.getAmount();
        repayment.createdAt = now;
        return repayment;
    }
}

