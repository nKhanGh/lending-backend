package com.nkhang.lending;

import com.nkhang.lending.drawdown.CreditDrawdown;
import com.nkhang.lending.instalment.Instalment;
import com.nkhang.lending.instalment.InstalmentScheduleFactory;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InstalmentScheduleFactoryTest {
    private final InstalmentScheduleFactory factory = new InstalmentScheduleFactory();

    @Test
    void finalInstalmentAbsorbsRemainder() {
        CreditDrawdown drawdown = mock(CreditDrawdown.class);
        when(drawdown.getOriginalAmount()).thenReturn(new BigDecimal("100.01"));
        List<Instalment> schedule = factory.create(drawdown, LocalDate.of(2026, 11, 24));

        assertThat(schedule).extracting(Instalment::getAmount)
                .containsExactly(new BigDecimal("25.00"), new BigDecimal("25.00"),
                        new BigDecimal("25.00"), new BigDecimal("25.01"));
        assertThat(schedule).extracting(Instalment::getDueDate)
                .containsExactly(LocalDate.of(2026, 12, 24), LocalDate.of(2027, 1, 24),
                        LocalDate.of(2027, 2, 24), LocalDate.of(2027, 3, 24));
    }
}
