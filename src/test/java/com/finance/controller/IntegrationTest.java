package com.finance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finance.dto.request.LoginRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests — loads the full Spring application context.
 * Uses the embedded H2 database and real JWT flow.
 *
 * These tests validate the complete request-response cycle including:
 *   - Security filter chain
 *   - Controller → Service → Repository
 *   - Error handler output
 *
 * Tradeoff: Integration tests are slower than unit tests but catch wiring issues
 * that unit tests with mocks miss. We use @SpringBootTest for critical paths
 * (auth, RBAC) and unit tests for business logic.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Integration Tests")
class IntegrationTest {

    @Autowired MockMvc       mockMvc;
    @Autowired ObjectMapper  objectMapper;

    // ── Auth ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/auth/login → 200 with token for valid admin credentials")
    void login_validAdmin_returns200WithToken() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setEmail("admin@finance.com");
        req.setPassword("Admin@123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    @DisplayName("POST /api/auth/login → 401 for wrong password")
    void login_wrongPassword_returns401() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setEmail("admin@finance.com");
        req.setPassword("WrongPassword");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    @DisplayName("POST /api/auth/login → 400 on missing fields")
    void login_missingFields_returns400WithValidationErrors() throws Exception {
        // Empty body
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors").isMap());
    }

    // ── RBAC — Unauthenticated ────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/records → 401 when no token provided")
    void getRecords_noToken_returns401() throws Exception {
        mockMvc.perform(get("/api/records"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/dashboard/summary → 401 when no token provided")
    void getDashboard_noToken_returns401() throws Exception {
        mockMvc.perform(get("/api/dashboard/summary"))
                .andExpect(status().isUnauthorized());
    }

    // ── RBAC — Role enforcement ───────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/users → 403 when authenticated as VIEWER")
    void getAllUsers_viewerRole_returns403() throws Exception {
        String viewerToken = loginAndGetToken("viewer@finance.com", "Viewer@123");

        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/dashboard/summary → 403 for VIEWER (basic endpoint available instead)")
    void getDashboardSummary_viewerRole_returns403() throws Exception {
        String viewerToken = loginAndGetToken("viewer@finance.com", "Viewer@123");

        mockMvc.perform(get("/api/dashboard/summary")
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/dashboard/basic → 200 for VIEWER role")
    void getBasicSummary_viewerRole_returns200() throws Exception {
        String viewerToken = loginAndGetToken("viewer@finance.com", "Viewer@123");

        mockMvc.perform(get("/api/dashboard/basic")
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncome").isNumber())
                .andExpect(jsonPath("$.netBalance").isNumber());
    }

    @Test
    @DisplayName("GET /api/records → 200 for VIEWER role (read is permitted)")
    void getRecords_viewerRole_returns200() throws Exception {
        String viewerToken = loginAndGetToken("viewer@finance.com", "Viewer@123");

        mockMvc.perform(get("/api/records")
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @DisplayName("POST /api/records → 403 for VIEWER (write not permitted)")
    void createRecord_viewerRole_returns403() throws Exception {
        String viewerToken = loginAndGetToken("viewer@finance.com", "Viewer@123");

        String body = """
                {
                  "amount": 1000.00,
                  "type": "INCOME",
                  "category": "Test",
                  "recordDate": "2024-03-01"
                }
                """;

        mockMvc.perform(post("/api/records")
                        .header("Authorization", "Bearer " + viewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/dashboard/summary → 200 for ANALYST role")
    void getDashboardSummary_analystRole_returns200() throws Exception {
        String analystToken = loginAndGetToken("analyst@finance.com", "Analyst@123");

        mockMvc.perform(get("/api/dashboard/summary")
                        .header("Authorization", "Bearer " + analystToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncome").isNumber())
                .andExpect(jsonPath("$.monthlyTrends").isArray());
    }

    @Test
    @DisplayName("GET /api/users/me → 200 for any authenticated user")
    void getMyProfile_anyRole_returns200() throws Exception {
        String analystToken = loginAndGetToken("analyst@finance.com", "Analyst@123");

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + analystToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("analyst@finance.com"));
    }

    // ── Not Found ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/records/99999 → 404 for non-existent record")
    void getRecord_notFound_returns404() throws Exception {
        String adminToken = loginAndGetToken("admin@finance.com", "Admin@123");

        mockMvc.perform(get("/api/records/99999")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // ── Full CRUD flow ────────────────────────────────────────────────────────

    @Test
    @DisplayName("Admin can create, read, update and delete a record")
    void adminCrudFlow_completeCycle() throws Exception {
        String adminToken = loginAndGetToken("admin@finance.com", "Admin@123");

        // 1. Create
        String createBody = """
                {
                  "amount": 7500.00,
                  "type": "INCOME",
                  "category": "Consulting",
                  "recordDate": "2024-03-15",
                  "description": "Integration test record"
                }
                """;

        MvcResult createResult = mockMvc.perform(post("/api/records")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn();

        Integer id = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("id").asInt();
        assertThat(id).isGreaterThan(0);

        // 2. Read
        mockMvc.perform(get("/api/records/" + id)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category").value("Consulting"));

        // 3. Update (partial — only description)
        mockMvc.perform(patch("/api/records/" + id)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "description": "Updated by integration test" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Updated by integration test"))
                .andExpect(jsonPath("$.category").value("Consulting")); // unchanged

        // 4. Delete
        mockMvc.perform(delete("/api/records/" + id)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        // 5. Verify it's gone
        mockMvc.perform(get("/api/records/" + id)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private String loginAndGetToken(String email, String password) throws Exception {
        LoginRequest req = new LoginRequest();
        req.setEmail(email);
        req.setPassword(password);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("token").asText();
    }
}
