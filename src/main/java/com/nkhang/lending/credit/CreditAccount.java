package com.nkhang.lending.credit;

import com.nkhang.lending.application.DemoUser;
import com.nkhang.lending.common.exception.AppException;
import com.nkhang.lending.common.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Entity
@Table(name = "credit_accounts")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreditAccount {
    @Id
    UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    DemoUser user;

    @Column(name = "total_limit", nullable = false, precision = 19, scale = 2)
    BigDecimal totalLimit;

    @Column(name = "used_limit", nullable = false, precision = 19, scale = 2)
    BigDecimal usedLimit;

    @Column(nullable = false, length = 3)
    String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    CreditAccountStatus status;

    @Version
    long version;

    public static CreditAccount active(DemoUser user, BigDecimal limit) {
        CreditAccount account = new CreditAccount();
        account.id = UUID.randomUUID();
        account.user = user;
        account.totalLimit = limit;
        account.usedLimit = new BigDecimal("0.00");
        account.currency = "SGD";
        account.status = CreditAccountStatus.ACTIVE;
        return account;
    }

    public BigDecimal getAvailableLimit() {
        return totalLimit.subtract(usedLimit);
    }

    public void utilize(BigDecimal amount) {
        if (status != CreditAccountStatus.ACTIVE || getAvailableLimit().compareTo(amount) < 0) {
            throw new AppException(ErrorCode.INSUFFICIENT_LIMIT);
        }
        usedLimit = usedLimit.add(amount);
    }

    public void repay(BigDecimal amount) {
        if (usedLimit.compareTo(amount) < 0) {
            throw new IllegalStateException("Repayment exceeds used limit");
        }
        usedLimit = usedLimit.subtract(amount);
    }
}
