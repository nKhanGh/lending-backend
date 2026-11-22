package com.nkhang.lending;

import com.nkhang.lending.catalog.ProductRepository;
import com.nkhang.lending.credit.CreditAccount;
import com.nkhang.lending.credit.CreditAccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class DataModelIntegrationTest extends PostgresIntegrationTest {

    private static final UUID DEMO_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Autowired
    ProductRepository productRepository;

    @Autowired
    CreditAccountRepository creditAccountRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    void resetFinancialState() {
        jdbcTemplate.update("delete from repayments");
        jdbcTemplate.update("delete from instalments");
        jdbcTemplate.update("delete from credit_reservations");
        jdbcTemplate.update("delete from orders");
        jdbcTemplate.update("delete from idempotency_records");
        jdbcTemplate.update("""
                update credit_accounts
                set outstanding_amount = 0.00, reserved_amount = 0.00, version = 0
                where user_id = ?
                """, DEMO_USER_ID);
    }

    @Test
    void flywaySeedsProductsAndDemoCreditAccount() {
        assertThat(productRepository.findAllByActiveTrueOrderByNameAsc()).hasSize(8);

        CreditAccount account = creditAccountRepository.findByUserId(DEMO_USER_ID).orElseThrow();
        assertThat(account.getTotalLimit()).isEqualByComparingTo("500.00");
        assertThat(account.getOutstandingAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(account.getReservedAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(account.getAvailableCredit()).isEqualByComparingTo("500.00");
    }
}
