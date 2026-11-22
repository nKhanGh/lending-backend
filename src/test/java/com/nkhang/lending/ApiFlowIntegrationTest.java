package com.nkhang.lending;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "lending.reservation.cleanup-delay=1h")
@AutoConfigureMockMvc
class ApiFlowIntegrationTest extends PostgresIntegrationTest {

    private static final UUID DEMO_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

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
    void completesPayLaterFlowThroughHttpApi() throws Exception {
        String checkoutJson = mockMvc.perform(post("/api/checkouts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "api-flow-checkout-001")
                        .content("""
                                {"productId":"00000000-0000-0000-0000-000000000201","quantity":1}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.result.status").value("RESERVED"))
                .andExpect(jsonPath("$.result.availableCredit").value(380.00))
                .andReturn().getResponse().getContentAsString();
        JsonNode checkout = objectMapper.readTree(checkoutJson);
        String checkoutId = checkout.at("/result/checkoutId").asText();

        String confirmJson = mockMvc.perform(post("/api/checkouts/{id}/confirm", checkoutId)
                        .header("Idempotency-Key", "api-flow-confirm-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.result.instalments", hasSize(4)))
                .andReturn().getResponse().getContentAsString();
        JsonNode confirmed = objectMapper.readTree(confirmJson);
        String instalmentId = confirmed.at("/result/instalments/0/id").asText();

        mockMvc.perform(post("/api/instalments/{id}/pay", instalmentId)
                        .header("Idempotency-Key", "api-flow-repay-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.status").value("PAID"))
                .andExpect(jsonPath("$.result.outstandingAmount").value(90.00))
                .andExpect(jsonPath("$.result.availableCredit").value(410.00));

        mockMvc.perform(get("/api/orders/{id}", checkoutId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.instalments[0].status").value("PAID"));

        mockMvc.perform(get("/api/repayments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result", hasSize(1)))
                .andExpect(jsonPath("$.result[0].amount").value(30.00));

        String settlementJson = mockMvc.perform(post("/api/orders/{id}/settle", checkoutId)
                        .header("Idempotency-Key", "api-flow-settle-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.instalmentsPaid").value(3))
                .andExpect(jsonPath("$.result.amount").value(90.00))
                .andExpect(jsonPath("$.result.outstandingAmount").value(0.00))
                .andExpect(jsonPath("$.result.availableCredit").value(500.00))
                .andReturn().getResponse().getContentAsString();

        String retryJson = mockMvc.perform(post("/api/orders/{id}/settle", checkoutId)
                        .header("Idempotency-Key", "api-flow-settle-001"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(objectMapper.readTree(retryJson).get("result"))
                .isEqualTo(objectMapper.readTree(settlementJson).get("result"));

        mockMvc.perform(get("/api/repayments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result", hasSize(4)));
    }
}
