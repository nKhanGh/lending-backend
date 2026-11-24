package com.nkhang.lending;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class FlywayUpgradeIntegrationTest extends PostgresIntegrationTest {
    @Autowired DataSource dataSource;

    @Test
    void upgradesExistingDrawdownAndRepaymentsWithoutLosingBalances() {
        String schema = "upgrade_" + UUID.randomUUID().toString().replace("-", "");
        jdbcTemplate.execute("create schema " + schema);
        try {
            Flyway.configure().dataSource(dataSource).schemas(schema).defaultSchema(schema)
                    .locations("classpath:db/migration").target(MigrationVersion.fromVersion("2"))
                    .load().migrate();
            String user = "'00000000-0000-0000-0000-000000000001'";
            String account = "'10000000-0000-0000-0000-000000000001'";
            String drawdown = "'20000000-0000-0000-0000-000000000001'";
            jdbcTemplate.execute("insert into " + schema + ".credit_accounts " +
                    "(id,user_id,total_limit,used_limit,currency,status,version) values (" +
                    account + "," + user + ",500,50,'SGD','ACTIVE',0)");
            jdbcTemplate.execute("insert into " + schema + ".credit_drawdowns " +
                    "(id,account_id,original_amount,outstanding_principal,status,created_at) values (" +
                    drawdown + "," + account + ",100,50,'ACTIVE',now())");
            for (int sequence = 1; sequence <= 4; sequence++) {
                String id = "'30000000-0000-0000-0000-00000000000" + sequence + "'";
                String status = sequence <= 2 ? "'PAID'" : "'UNPAID'";
                String paidAt = sequence <= 2 ? "now()" : "null";
                jdbcTemplate.execute("insert into " + schema + ".instalments " +
                        "(id,drawdown_id,sequence_no,amount,due_date,status,paid_at) values (" +
                        id + "," + drawdown + "," + sequence + ",25,current_date + " + sequence +
                        "," + status + "," + paidAt + ")");
                if (sequence <= 2) {
                    String repaymentId = "'40000000-0000-0000-0000-00000000000" + sequence + "'";
                    jdbcTemplate.execute("insert into " + schema + ".repayment_transactions " +
                            "(id,instalment_id,amount,status,paid_at) values (" +
                            repaymentId + "," + id + ",25,'SUCCESS',now())");
                }
            }
            jdbcTemplate.execute("insert into " + schema + ".idempotency_records " +
                    "(id,operation,idempotency_key,request_fingerprint,resource_id,http_status,status,created_at,updated_at) " +
                    "values ('50000000-0000-0000-0000-000000000001','CREATE_DRAWDOWN','legacy-key'," +
                    "'" + "0".repeat(64) + "'," + drawdown + ",201,'COMPLETED',now(),now())");

            Flyway.configure().dataSource(dataSource).schemas(schema).defaultSchema(schema)
                    .locations("classpath:db/migration").load().migrate();

            assertThat(jdbcTemplate.queryForObject(
                    "select count(*) from " + schema + ".ledger_journals", Integer.class)).isEqualTo(3);
            assertThat(jdbcTemplate.queryForObject(
                    "select count(*) from " + schema + ".ledger_entries", Integer.class)).isEqualTo(6);
            assertThat(jdbcTemplate.queryForObject(
                    "select sum(case when e.direction='DEBIT' then e.amount else -e.amount end) " +
                    "from " + schema + ".ledger_entries e join " + schema + ".ledger_journals j on j.id=e.journal_id " +
                    "where e.account_code='CUSTOMER_RECEIVABLE'", java.math.BigDecimal.class))
                    .isEqualByComparingTo("50.00");
            assertThat(jdbcTemplate.queryForObject(
                    "select sum(paid_amount) from " + schema + ".instalments", java.math.BigDecimal.class))
                    .isEqualByComparingTo("50.00");
            assertThat(jdbcTemplate.queryForObject(
                    "select count(*) from " + schema + ".idempotency_records where user_id=" + user,
                    Integer.class)).isEqualTo(1);
        } finally {
            jdbcTemplate.execute("drop schema " + schema + " cascade");
        }
    }
}
