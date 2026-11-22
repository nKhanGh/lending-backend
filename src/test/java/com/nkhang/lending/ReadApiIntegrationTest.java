package com.nkhang.lending;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ReadApiIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void returnsSeededProductCatalogue() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.result", hasSize(8)))
                .andExpect(jsonPath("$.result[0].currency").value("SGD"));
    }

    @Test
    void returnsInitialCreditDashboard() throws Exception {
        mockMvc.perform(get("/api/credit-account"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.totalLimit").value(500.00))
                .andExpect(jsonPath("$.result.outstandingAmount").value(0.00))
                .andExpect(jsonPath("$.result.reservedAmount").value(0.00))
                .andExpect(jsonPath("$.result.availableCredit").value(500.00))
                .andExpect(jsonPath("$.result.currency").value("SGD"));
    }
}
