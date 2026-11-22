package com.nkhang.lending.credit;

import com.nkhang.lending.common.exception.AppException;
import com.nkhang.lending.common.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
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

    @Column(name = "user_id", nullable = false, unique = true)
    UUID userId;

    @Column(name = "total_limit", nullable = false, precision = 18, scale = 2)
    BigDecimal totalLimit;

    @Column(name = "outstanding_amount", nullable = false, precision = 18, scale = 2)
    BigDecimal outstandingAmount;

    @Column(name = "reserved_amount", nullable = false, precision = 18, scale = 2)
    BigDecimal reservedAmount;

    @Column(nullable = false, length = 3)
    String currency;

    @Version
    long version;

    public BigDecimal getAvailableCredit() {
        return totalLimit.subtract(outstandingAmount).subtract(reservedAmount);
    }

    public void reserve(BigDecimal amount) {
        requirePositive(amount);
        if (getAvailableCredit().compareTo(amount) < 0) {
            throw new AppException(ErrorCode.INSUFFICIENT_CREDIT);
        }
        reservedAmount = reservedAmount.add(amount);
    }

    public void release(BigDecimal amount) {
        requirePositive(amount);
        if (reservedAmount.compareTo(amount) < 0) {
            throw new IllegalStateException("Cannot release more than the reserved amount");
        }
        reservedAmount = reservedAmount.subtract(amount);
    }

    public void capture(BigDecimal amount) {
        release(amount);
        outstandingAmount = outstandingAmount.add(amount);
    }

    public void repay(BigDecimal amount) {
        requirePositive(amount);
        if (outstandingAmount.compareTo(amount) < 0) {
            throw new IllegalStateException("Cannot repay more than the outstanding amount");
        }
        outstandingAmount = outstandingAmount.subtract(amount);
    }

    private void requirePositive(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
    }
}

