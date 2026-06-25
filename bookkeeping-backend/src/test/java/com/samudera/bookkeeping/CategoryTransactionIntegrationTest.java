package com.samudera.bookkeeping;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
@Sql(scripts = {"classpath:sql/test-reset.sql", "classpath:sql/test-schema.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class CategoryTransactionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void categoryEndpointsShouldRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void categoryCrudShouldWorkForAuthenticatedUser() throws Exception {
        String token = registerAndGetToken("category_user");

        Long categoryId = createCategory(token, "Food", 2);

        mockMvc.perform(get("/api/categories")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(categoryId))
                .andExpect(jsonPath("$.data[0].name").value("Food"));

        mockMvc.perform(put("/api/categories/{id}", categoryId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" +
                                "\"name\":\"Meals\"," +
                                "\"type\":2," +
                                "\"icon\":\"fork\"," +
                                "\"color\":\"#FF6600\"," +
                                "\"sortOrder\":2" +
                                "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Meals"))
                .andExpect(jsonPath("$.data.icon").value("fork"));

        mockMvc.perform(delete("/api/categories/{id}", categoryId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/categories")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void categoryShouldBeScopedToItsOwner() throws Exception {
        String ownerToken = registerAndGetToken("owner_user");
        String otherToken = registerAndGetToken("other_user");
        Long categoryId = createCategory(ownerToken, "Transport", 2);

        mockMvc.perform(put("/api/categories/{id}", categoryId)
                        .header("Authorization", bearer(otherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" +
                                "\"name\":\"Should Fail\"," +
                                "\"type\":2," +
                                "\"sortOrder\":0" +
                                "}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void transactionCrudAndFiltersShouldWorkForAuthenticatedUser() throws Exception {
        String token = registerAndGetToken("transaction_user");
        Long expenseCategoryId = createCategory(token, "Food", 2);
        Long incomeCategoryId = createCategory(token, "Salary", 1);

        Long firstTransactionId = createTransaction(token, expenseCategoryId, 2, "50.25", "2026-06-01", "Lunch");
        createTransaction(token, incomeCategoryId, 1, "5000.00", "2026-06-15", "Monthly salary");
        createTransaction(token, expenseCategoryId, 2, "25.00", "2026-06-20", "Dinner");

        mockMvc.perform(get("/api/transactions")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3));

        mockMvc.perform(get("/api/transactions")
                        .header("Authorization", bearer(token))
                        .param("type", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));

        mockMvc.perform(get("/api/transactions")
                        .header("Authorization", bearer(token))
                        .param("categoryId", String.valueOf(expenseCategoryId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));

        mockMvc.perform(get("/api/transactions")
                        .header("Authorization", bearer(token))
                        .param("startDate", "2026-06-10")
                        .param("endDate", "2026-06-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));

        mockMvc.perform(put("/api/transactions/{id}", firstTransactionId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" +
                                "\"categoryId\":" + expenseCategoryId + "," +
                                "\"type\":2," +
                                "\"amount\":60.50," +
                                "\"transactionDate\":\"2026-06-01\"," +
                                "\"note\":\"Updated lunch\"" +
                                "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.amount").value(60.50))
                .andExpect(jsonPath("$.data.note").value("Updated lunch"));

        mockMvc.perform(delete("/api/transactions/{id}", firstTransactionId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/transactions")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    void transactionShouldRejectCategoryOwnedByAnotherUser() throws Exception {
        String ownerToken = registerAndGetToken("tx_owner");
        String otherToken = registerAndGetToken("tx_other");
        Long categoryId = createCategory(ownerToken, "Owner Category", 2);

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", bearer(otherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" +
                                "\"categoryId\":" + categoryId + "," +
                                "\"type\":2," +
                                "\"amount\":12.50," +
                                "\"transactionDate\":\"2026-06-25\"," +
                                "\"note\":\"Should fail\"" +
                                "}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Category not found"));
    }

    private String registerAndGetToken(String username) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" +
                                "\"username\":\"" + username + "\"," +
                                "\"password\":\"secret123\"," +
                                "\"nickname\":\"" + username + "\"," +
                                "\"email\":\"" + username + "@example.com\"" +
                                "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        JsonNode jsonNode = objectMapper.readTree(result.getResponse().getContentAsString());
        return jsonNode.path("data").path("token").asText();
    }

    private Long createCategory(String token, String name, Integer type) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/categories")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" +
                                "\"name\":\"" + name + "\"," +
                                "\"type\":" + type + "," +
                                "\"icon\":\"icon\"," +
                                "\"color\":\"#123456\"," +
                                "\"sortOrder\":1" +
                                "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        JsonNode jsonNode = objectMapper.readTree(result.getResponse().getContentAsString());
        return jsonNode.path("data").path("id").asLong();
    }

    private Long createTransaction(String token, Long categoryId, Integer type, String amount, String date, String note) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/transactions")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" +
                                "\"categoryId\":" + categoryId + "," +
                                "\"type\":" + type + "," +
                                "\"amount\":" + amount + "," +
                                "\"transactionDate\":\"" + date + "\"," +
                                "\"note\":\"" + note + "\"" +
                                "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        JsonNode jsonNode = objectMapper.readTree(result.getResponse().getContentAsString());
        Long transactionId = jsonNode.path("data").path("id").asLong();
        assertThat(transactionId).isPositive();
        return transactionId;
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}