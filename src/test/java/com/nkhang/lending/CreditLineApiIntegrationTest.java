package com.nkhang.lending;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "00000000-0000-0000-0000-000000000001", roles = "CUSTOMER")
class CreditLineApiIntegrationTest extends PostgresIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    void completesDemoFlowThroughHttp() throws Exception {
        mockMvc.perform(get("/api/credit-account")).andExpect(status().isNotFound());
        mockMvc.perform(post("/api/credit-applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"monthlyNetIncome":3000.00,"monthlyLivingExpenses":1400.00,"monthlyDebtPayments":500.00}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.result.decision").value("APPROVED"))
                .andExpect(jsonPath("$.result.reasonCode").value("APPROVED_TIER_1000"))
                .andExpect(jsonPath("$.result.monthlySurplus").value(1100.00));
        mockMvc.perform(get("/api/credit-applications/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.policyVersion").value("DEMO_V1"));

        String drawdownJson = mockMvc.perform(post("/api/credit-drawdowns")
                        .header("Idempotency-Key", "api-drawdown-800")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" + "\"amount\":800.00}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.result.instalments", hasSize(4)))
                .andExpect(jsonPath("$.result.availableLimit").value(200.00))
                .andReturn().getResponse().getContentAsString();
        JsonNode drawdown = objectMapper.readTree(drawdownJson).get("result");
        String drawdownId = drawdown.get("drawdownId").asText();
        String firstInstalmentId = drawdown.get("instalments").get(0).get("id").asText();

        String retryJson = mockMvc.perform(post("/api/credit-drawdowns")
                        .header("Idempotency-Key", "api-drawdown-800")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" + "\"amount\":800.00}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        assertThat(objectMapper.readTree(retryJson).get("result")).isEqualTo(drawdown);

        String attemptJson = mockMvc.perform(post("/api/instalments/{id}/repay", firstInstalmentId)
                        .header("Idempotency-Key", "api-repay-first"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.result.status").value("PENDING"))
                .andReturn().getResponse().getContentAsString();
        String attemptId = objectMapper.readTree(attemptJson).get("result").get("paymentAttemptId").asText();
        mockMvc.perform(post("/api/admin/payment-attempts/{id}/settle", attemptId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"outcome\":\"SUCCEEDED\",\"providerEventId\":\"api-event-one\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/admin/payment-attempts/{id}/settle", attemptId)
                        .with(user("00000000-0000-0000-0000-000000000001").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"outcome\":\"SUCCEEDED\",\"providerEventId\":\"api-event-one\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.status").value("SUCCEEDED"));
        mockMvc.perform(get("/api/credit-drawdowns/{id}/repayments", drawdownId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result", hasSize(1)));
        mockMvc.perform(get("/api/credit-account"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.usedLimit").value(600.00));
    }

    @Test
    void validatesApplicationAndDrawdownRequests() throws Exception {
        mockMvc.perform(post("/api/credit-applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" + "\"monthlyNetIncome\":-1,\"monthlyLivingExpenses\":0}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/credit-applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"monthlyNetIncome":3000,"monthlyLivingExpenses":1400,"monthlyDebtPayments":500,"currency":"USD"}
                                """))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/credit-drawdowns")
                        .header("Idempotency-Key", "small")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" + "\"amount\":0.01}"))
                .andExpect(status().isBadRequest());
    }
}
