package com.nkhang.lending;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

public abstract class PostgresIntegrationTest {
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("creditline_test")
            .withUsername("lending")
            .withPassword("lending");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    void resetData() {
        jdbcTemplate.execute("delete from payment_attempts");
        jdbcTemplate.execute("delete from repayment_transactions");
        jdbcTemplate.execute("delete from instalments");
        jdbcTemplate.execute("delete from credit_drawdowns");
        jdbcTemplate.execute("delete from idempotency_records");
        jdbcTemplate.execute("delete from credit_accounts");
        jdbcTemplate.execute("delete from credit_applications");
        jdbcTemplate.execute("delete from users where id <> '00000000-0000-0000-0000-000000000001'");
    }
}
