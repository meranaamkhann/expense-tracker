package com.asad.expensetracker.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end coverage of the parts that matter most in a money app: you can't get in without
 * a valid token, and you can never see another user's data.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthAndIsolationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void protectedEndpointRejectsRequestsWithNoToken() throws Exception {
        mockMvc.perform(get("/api/expenses"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void registerLoginAndAccessProfile() throws Exception {
        String email = "flow-" + System.nanoTime() + "@example.com";
        String accessToken = registerAndGetAccessToken(email, "SuperSecret123");

        mockMvc.perform(get("/api/users/profile").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));

        // Registering seeds default categories, so a fresh user already has some to pick from.
        mockMvc.perform(get("/api/categories").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThan(0)));
    }

    @Test
    void duplicateRegistrationIsRejected() throws Exception {
        String email = "dupe-" + System.nanoTime() + "@example.com";
        registerAndGetAccessToken(email, "SuperSecret123");

        String body = """
                {"name":"Someone Else","email":"%s","password":"AnotherPass123"}
                """.formatted(email);

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void usersCannotSeeOrModifyEachOthersExpenses() throws Exception {
        String userAToken = registerAndGetAccessToken("a-" + System.nanoTime() + "@example.com", "PasswordA123");
        String userBToken = registerAndGetAccessToken("b-" + System.nanoTime() + "@example.com", "PasswordB123");

        Long categoryIdForA = firstCategoryId(userAToken);

        String expensePayload = """
                {"title":"Coffee","amount":4.50,"categoryId":%d,"date":"2026-08-01","kind":"expense"}
                """.formatted(categoryIdForA);

        MvcResult createResult = mockMvc.perform(post("/api/expenses")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(expensePayload))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode created = objectMapper.readTree(createResult.getResponse().getContentAsString());
        long expenseId = created.get("id").asLong();

        // User B must not see it in their own list...
        mockMvc.perform(get("/api/expenses").header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + expenseId + ")]").isEmpty());

        // ...and must not be able to delete it by guessing the id.
        mockMvc.perform(delete("/api/expenses/" + expenseId).header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isNotFound());

        // The owner can still see and delete it fine.
        mockMvc.perform(delete("/api/expenses/" + expenseId).header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void forgotPasswordAlwaysReturnsOkEvenForUnknownEmail() throws Exception {
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"definitely-not-registered@example.com\"}"))
                .andExpect(status().isOk());
    }

    private String registerAndGetAccessToken(String email, String password) throws Exception {
        String body = """
                {"name":"Test User","email":"%s","password":"%s"}
                """.formatted(email, password);

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        String token = json.get("accessToken").asText();
        assertThat(token).isNotBlank();
        return token;
    }

    private Long firstCategoryId(String accessToken) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/categories").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get(0).get("id").asLong();
    }
}
