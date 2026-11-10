package com.printcreations.printcreations.quote;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "PRINT_CREATIONS_ADMIN_PASSWORD=" + AdminQuoteLookupTests.TEST_PASSWORD)
@ActiveProfiles("local-admin")
@AutoConfigureMockMvc
class AdminQuoteLookupTests {

    static final String TEST_PASSWORD = "pc008-integration-test-only";
    private static final String LOOKUP_URL = "/api/admin/quotes/{referenceNumber}";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void adminCanReadTheQuoteSubmittedByAGuest() throws Exception {
        UUID referenceNumber = submitGuestQuote();

        mockMvc.perform(get(LOOKUP_URL, referenceNumber)
                        .with(httpBasic("admin", TEST_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(jsonPath("$.referenceNumber").value(referenceNumber.toString()))
                .andExpect(jsonPath("$.email").value("admin-lookup@example.com"))
                .andExpect(jsonPath("$.productId").value(12))
                .andExpect(jsonPath("$.status").value("NEW"))
                .andExpect(jsonPath("$.rushRequested").value(true))
                .andExpect(jsonPath("$.rushApproved").value(false))
                .andExpect(jsonPath("$.marketingOptIn").value(false));
    }

    @Test
    void rejectsMissingOrInvalidCredentials() throws Exception {
        UUID referenceNumber = submitGuestQuote();

        mockMvc.perform(get(LOOKUP_URL, referenceNumber))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get(LOOKUP_URL, referenceNumber)
                        .with(httpBasic("admin", "incorrect-test-password")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deniesAccessToStaff() throws Exception {
        UUID referenceNumber = submitGuestQuote();

        mockMvc.perform(get(LOOKUP_URL, referenceNumber)
                        .with(user("test-staff").roles("STAFF")))
                .andExpect(status().isForbidden());
    }

    @Test
    void returnsNotFoundToAdminForAnUnknownReference() throws Exception {
        mockMvc.perform(get(LOOKUP_URL, UUID.randomUUID())
                        .with(httpBasic("admin", TEST_PASSWORD)))
                .andExpect(status().isNotFound());
    }

    private UUID submitGuestQuote() throws Exception {
        String receipt = mockMvc.perform(post("/api/quote-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Integration",
                                  "lastName": "Customer",
                                  "phone": "415-555-0100",
                                  "email": "admin-lookup@example.com",
                                  "productId": 12,
                                  "quantity": 250,
                                  "fulfillmentMethod": "PICKUP",
                                  "rushRequested": true,
                                  "rushNotes": "Please confirm rush availability.",
                                  "marketingOptIn": false
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return UUID.fromString(JsonPath.read(receipt, "$.referenceNumber"));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class DatabaseConfig {

        @Bean
        @ServiceConnection
        PostgreSQLContainer postgres() {
            return new PostgreSQLContainer("postgres:18.6")
                    .withDatabaseName("print_creations_admin_test");
        }
    }
}