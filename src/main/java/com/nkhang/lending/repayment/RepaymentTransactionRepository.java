package com.nkhang.lending.repayment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface RepaymentTransactionRepository extends JpaRepository<RepaymentTransaction, UUID> {
    @Query("select repayment from RepaymentTransaction repayment where repayment.instalment.drawdown.id = :drawdownId order by repayment.paidAt desc")
    List<RepaymentTransaction> findAllByDrawdownId(@Param("drawdownId") UUID drawdownId);
}
