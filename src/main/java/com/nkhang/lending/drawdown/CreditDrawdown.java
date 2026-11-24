package com.nkhang.lending.drawdown;

import com.nkhang.lending.credit.CreditAccount;
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
import java.util.UUID;

@Getter
@Entity
@Table(name = "credit_drawdowns")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreditDrawdown {
    @Id
    UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    CreditAccount account;

    @Column(name = "original_amount", nullable = false, precision = 19, scale = 2)
    BigDecimal originalAmount;

    @Column(name = "outstanding_principal", nullable = false, precision = 19, scale = 2)
    BigDecimal outstandingPrincipal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    DrawdownStatus status;

    @Column(name = "created_at", nullable = false)
    Instant createdAt;

    public static CreditDrawdown active(CreditAccount account, BigDecimal amount, Instant now) {
        CreditDrawdown drawdown = new CreditDrawdown();
        drawdown.id = UUID.randomUUID();
        drawdown.account = account;
        drawdown.originalAmount = amount;
        drawdown.outstandingPrincipal = amount;
        drawdown.status = DrawdownStatus.ACTIVE;
        drawdown.createdAt = now;
        return drawdown;
    }

    public void repay(BigDecimal amount) {
        if (status != DrawdownStatus.ACTIVE || outstandingPrincipal.compareTo(amount) < 0) {
            throw new IllegalStateException("Invalid drawdown repayment");
        }
        outstandingPrincipal = outstandingPrincipal.subtract(amount);
        if (outstandingPrincipal.signum() == 0) {
            status = DrawdownStatus.PAID_OFF;
        }
    }
}
