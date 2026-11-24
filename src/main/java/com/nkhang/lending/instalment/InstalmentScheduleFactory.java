package com.nkhang.lending.instalment;

import com.nkhang.lending.drawdown.CreditDrawdown;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
public class InstalmentScheduleFactory {
    private static final int COUNT = 4;

    public List<Instalment> create(CreditDrawdown drawdown, LocalDate drawdownDate) {
        BigDecimal base = drawdown.getOriginalAmount().divide(BigDecimal.valueOf(COUNT), 2, RoundingMode.DOWN);
        BigDecimal finalAmount = drawdown.getOriginalAmount()
                .subtract(base.multiply(BigDecimal.valueOf(COUNT - 1L)));
        List<Instalment> schedule = new ArrayList<>(COUNT);
        for (int sequence = 1; sequence <= COUNT; sequence++) {
            schedule.add(Instalment.unpaid(drawdown, sequence,
                    sequence == COUNT ? finalAmount : base, drawdownDate.plusMonths(sequence)));
        }
        return List.copyOf(schedule);
    }
}
