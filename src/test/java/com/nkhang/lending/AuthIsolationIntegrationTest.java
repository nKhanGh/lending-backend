package com.nkhang.lending;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

@SpringBootTest
@AutoConfigureMockMvc
class AuthIsolationIntegrationTest extends PostgresIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    void registrationLoginAndOwnershipAreEnforced() throws Exception {
        mockMvc.perform(get("/api/credit-account")).andExpect(status().isUnauthorized());
        String firstEmail = "one-" + UUID.randomUUID() + "@example.test";
        String first = register(firstEmail);
        String second = register("two-" + UUID.randomUUID() + "@example.test");

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"Duplicate\",\"email\":\"" + firstEmail + "\",\"password\":\"password123\"}"))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + firstEmail + "\",\"password\":\"wrongpass\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + firstEmail + "\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.role").value("CUSTOMER"));
        mockMvc.perform(get("/api/auth/me").header("Authorization", bearer(first)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.email").value(firstEmail));
        mockMvc.perform(get("/api/admin/users").header("Authorization", bearer(first)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/audit-events").header("Authorization", bearer(first)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/credit-applications").header("Authorization", bearer(first))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monthlyNetIncome\":3000,\"monthlyLivingExpenses\":1400,\"monthlyDebtPayments\":500}"))
                .andExpect(status().isCreated());
        String drawdownJson = mockMvc.perform(post("/api/credit-drawdowns")
                        .header("Authorization", bearer(first)).header("Idempotency-Key", "shared-key")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"amount\":100.00}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        JsonNode firstDrawdown = objectMapper.readTree(drawdownJson).get("result");
        String id = firstDrawdown.get("drawdownId").asText();
        String instalmentId = firstDrawdown.get("instalments").get(0).get("id").asText();

        mockMvc.perform(get("/api/credit-account").header("Authorization", bearer(second)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/credit-drawdowns").header("Authorization", bearer(second)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.result.length()").value(0));
        mockMvc.perform(get("/api/credit-drawdowns/{id}", id).header("Authorization", bearer(second)))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/instalments/{id}/repay", instalmentId)
                        .header("Authorization", bearer(second)).header("Idempotency-Key", "shared-key"))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/credit-applications").header("Authorization", bearer(second))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monthlyNetIncome\":3000,\"monthlyLivingExpenses\":1400,\"monthlyDebtPayments\":500}"))
                .andExpect(status().isCreated());
        String secondDrawdownJson = mockMvc.perform(post("/api/credit-drawdowns")
                        .header("Authorization", bearer(second)).header("Idempotency-Key", "shared-key")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"amount\":100.00}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        assertThat(objectMapper.readTree(secondDrawdownJson).get("result").get("drawdownId").asText())
                .isNotEqualTo(id);
    }

    private String register(String email) throws Exception {
        String response = mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"Demo\",\"email\":\"" + email + "\",\"password\":\"password123\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("result").get("accessToken").asText();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000001", roles = "ADMIN")
    void adminRouteRequiresAdminRole() throws Exception {
        mockMvc.perform(get("/api/admin/users")).andExpect(status().isOk());
        mockMvc.perform(get("/api/admin/audit-events")).andExpect(status().isOk());
    }

    @Test
    void adminEvidenceIsSnapshottedIntoCreditDecision() throws Exception {
        String token = register("evidence-" + UUID.randomUUID() + "@example.test");
        String userId = objectMapper.readTree(mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", bearer(token))).andReturn().getResponse().getContentAsString())
                .get("result").get("userId").asText();
        String profile = "{\"verifiedMonthlyIncome\":2000.00,\"latePayments12m\":0}";
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(
                        "/api/admin/credit-profiles/{id}", userId)
                        .header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON).content(profile))
                .andExpect(status().isForbidden());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(
                        "/api/admin/credit-profiles/{id}", userId)
                        .with(user("00000000-0000-0000-0000-000000000001").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content(profile))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/credit-applications").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monthlyNetIncome\":3000,\"monthlyLivingExpenses\":1400,\"monthlyDebtPayments\":500}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.result.decision").value("REJECTED"))
                .andExpect(jsonPath("$.result.reasonCode").value("INCOME_NOT_VERIFIED"))
                .andExpect(jsonPath("$.result.verifiedMonthlyIncome").value(2000.00))
                .andExpect(jsonPath("$.result.policyVersion").value("DEMO_V2"));
    }
}
