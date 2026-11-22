package com.nkhang.lending.instalment;

import com.nkhang.lending.checkout.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
public class InstalmentScheduleFactory {

    private static final int INSTALMENT_COUNT = 4;

    public List<Instalment> create(Order order, LocalDate confirmationDate) {
        BigDecimal base = order.getTotalAmount()
                .divide(BigDecimal.valueOf(INSTALMENT_COUNT), 2, RoundingMode.DOWN);
        BigDecimal finalAmount = order.getTotalAmount()
                .subtract(base.multiply(BigDecimal.valueOf(INSTALMENT_COUNT - 1L)));

        List<Instalment> schedule = new ArrayList<>(INSTALMENT_COUNT);
        for (int sequence = 1; sequence <= INSTALMENT_COUNT; sequence++) {
            BigDecimal amount = sequence == INSTALMENT_COUNT ? finalAmount : base;
            schedule.add(Instalment.unpaid(
                    order,
                    sequence,
                    amount,
                    confirmationDate.plusMonths(sequence)));
        }
        return List.copyOf(schedule);
    }
}

