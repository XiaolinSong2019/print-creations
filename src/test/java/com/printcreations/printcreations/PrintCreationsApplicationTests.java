package com.printcreations.printcreations;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PrintCreationsApplicationTests {

    private static final String VALID_QUOTE = """
            {
              "firstName": "Avery",
              "lastName": "Example",
              "phone": "408-555-0100",
              "email": "avery.quote@example.com",
              "productId": 12,
              "quantity": 250,
              "fulfillmentMethod": "PICKUP",
              "companyName": "Example Studio",
              "neededBy": "2026-11-20",
              "productOptions": {"finish": "Matte"},
              "specialInstructions": "Please quote both sides.",
              "rushRequested": true,
              "rushRequestedCompletionDate": "2026-11-18",
              "rushNotes": "Please confirm the earliest pickup time."
            }
            """;

    @Autowired
    private JdbcClient jdbcClient;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void connectsToPostgreSql() {
        String databaseName = jdbcClient.sql("SELECT current_database()")
                .query(String.class)
                .single();

        assertEquals("print_creations_test", databaseName);
    }

    @Test
    void savesQuoteAndReturnsTheStoredReference() throws Exception {
        MvcResult result = postQuote(VALID_QUOTE)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.referenceNumber").isString())
                .andExpect(jsonPath("$.status").value("NEW"))
                .andReturn();

        String reference = JsonPath.read(
                result.getResponse().getContentAsString(), "$.referenceNumber"
        );

        Map<String, Object> saved = jdbcClient.sql("""
                SELECT email, product_id, quantity, fulfillment_method, status,
                       product_options ->> 'finish' AS finish,
                       rush_requested, rush_requested_completion_date, rush_notes,
                       rush_approved, rush_fee_amount, marketing_opt_in
                FROM quote_requests
                WHERE reference_number = :reference
                """)
                .param("reference", UUID.fromString(reference))
                .query()
                .singleRow();

        assertEquals("avery.quote@example.com", saved.get("email"));
        assertEquals(12L, saved.get("product_id"));
        assertEquals(250, saved.get("quantity"));
        assertEquals("PICKUP", saved.get("fulfillment_method"));
        assertEquals("NEW", saved.get("status"));
        assertEquals("Matte", saved.get("finish"));
        assertEquals(true, saved.get("rush_requested"));
        assertEquals("2026-11-18", saved.get("rush_requested_completion_date").toString());
        assertEquals("Please confirm the earliest pickup time.", saved.get("rush_notes"));
        assertEquals(false, saved.get("rush_approved"));
        assertNull(saved.get("rush_fee_amount"));
        assertEquals(false, saved.get("marketing_opt_in"));
    }

    @Test
    void rejectsInvalidQuotesWithoutInsertingRows() throws Exception {
        assertRejectedWithoutInsert(
                VALID_QUOTE.replace("\"phone\": \"408-555-0100\",", ""),
                "$.errors.phone"
        );
        assertRejectedWithoutInsert(
                VALID_QUOTE.replace("avery.quote@example.com", "not-an-email"),
                "$.errors.email"
        );
        assertRejectedWithoutInsert(
                VALID_QUOTE.replace("\"quantity\": 250", "\"quantity\": 0"),
                "$.errors.quantity"
        );
        assertRejectedWithoutInsert(
                VALID_QUOTE.replace("\"quantity\": 250", "\"quantity\": 2.5"),
                "$.detail"
        );
        assertRejectedWithoutInsert(
                VALID_QUOTE.replace("\"productId\": 12", "\"productId\": 999999"),
                "$.errors.productId"
        );
    }

    private void assertRejectedWithoutInsert(String body, String errorPath) throws Exception {
        long before = quoteCount();

        postQuote(body)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(errorPath).isNotEmpty());

        assertEquals(before, quoteCount());
    }

    private long quoteCount() {
        return jdbcClient.sql("SELECT count(*) FROM quote_requests")
                .query(Long.class)
                .single();
    }

    private ResultActions postQuote(String body) throws Exception {
        return mockMvc.perform(post("/api/quote-requests")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class PostgresConfiguration {

        @Bean
        @ServiceConnection
        PostgreSQLContainer postgresContainer() {
            return new PostgreSQLContainer("postgres:18.6")
                    .withDatabaseName("print_creations_test");
        }
    }
}