package com.printcreations.printcreations.quote;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "PRINT_CREATIONS_ADMIN_PASSWORD=" + AdminRushApprovalTests.TEST_PASSWORD)
@ActiveProfiles("local-admin")
@AutoConfigureMockMvc
class AdminRushApprovalTests {

    static final String TEST_PASSWORD = "pc009-integration-test-only";
    private static final String QUOTE_URL = "/api/admin/quotes/{referenceNumber}";
    private static final String RUSH_URL = QUOTE_URL + "/rush-approval";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcClient jdbcClient;

    @Autowired
    private RushApprovalService rushApprovalService;

    @Test
    void approvesAndClearsRushWithOneAuditPerEffectiveChange() throws Exception {
        UUID reference = submitQuote(true);
        quote(reference).andExpect(jsonPath("$.version").value(0));

        var csrfResult = mockMvc.perform(get("/api/admin/csrf").with(httpBasic("admin", TEST_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andReturn();
        var session = (MockHttpSession) csrfResult.getRequest().getSession(false);
        assertThat(session).isNotNull();
        String csrfBody = csrfResult.getResponse().getContentAsString();
        String headerName = JsonPath.read(csrfBody, "$.headerName");
        String token = JsonPath.read(csrfBody, "$.token");

        mockMvc.perform(rushRequest(reference, decision(true, "25.50", 0))
                        .session(session)
                        .header(headerName, token)
                        .with(httpBasic("admin", TEST_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rushApproved").value(true))
                .andExpect(jsonPath("$.version").value(1));

        adminPatch(reference, decision(true, "25.5", 0))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(1));

        var afterApproval = audit(reference);
        assertThat(afterApproval).hasSize(1);
        var approved = afterApproval.getFirst();
        assertThat(approved.actor()).isEqualTo("admin");
        assertThat(approved.oldVersion()).isZero();
        assertThat(approved.newVersion()).isEqualTo(1);
        assertThat(approved.oldApproved()).isFalse();
        assertThat(approved.newApproved()).isTrue();
        assertThat(approved.oldFee()).isNull();
        assertThat(approved.newFee()).isEqualByComparingTo("25.50");
        assertThat(approved.changedAt()).isNotNull();

        adminPatch(reference, decision(false, "null", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rushApproved").value(false))
                .andExpect(jsonPath("$.rushFeeAmount").doesNotExist())
                .andExpect(jsonPath("$.version").value(2));
        adminPatch(reference, decision(false, "null", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(2));

        var afterClear = audit(reference);
        assertThat(afterClear).hasSize(2);
        var cleared = afterClear.getLast();
        assertThat(cleared.oldApproved()).isTrue();
        assertThat(cleared.newApproved()).isFalse();
        assertThat(cleared.oldFee()).isEqualByComparingTo("25.50");
        assertThat(cleared.newFee()).isNull();
        quote(reference)
                .andExpect(jsonPath("$.version").value(2))
                .andExpect(jsonPath("$.status").value("NEW"))
                .andExpect(jsonPath("$.rushRequested").value(true))
                .andExpect(jsonPath("$.rushNotes").value("Need a Friday pickup."))
                .andExpect(jsonPath("$.marketingOptIn").value(false));
    }

    @Test
    void rejectsUnauthorizedWritesAndRequiresCsrf() throws Exception {
        UUID reference = submitQuote(true);
        String body = decision(true, "25", 0);

        mockMvc.perform(rushRequest(reference, body).with(csrf().asHeader()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(rushRequest(reference, body)
                        .with(csrf().asHeader())
                        .with(user("staff").roles("STAFF")))
                .andExpect(status().isForbidden());
        mockMvc.perform(rushRequest(reference, body).with(httpBasic("admin", TEST_PASSWORD)))
                .andExpect(status().isForbidden());

        assertUnchanged(reference);
    }

    @Test
    void rejectsInvalidFeesVersionsAndFields() throws Exception {
        UUID reference = submitQuote(true);
        for (String body : List.of(
                decision(true, "null", 0),
                decision(true, "-1", 0),
                decision(true, "1.001", 0),
                decision(true, "10000000000", 0),
                decision(false, "25", 0),
                decision(true, "25", -1),
                "{\"rushApproved\":true,\"rushFeeAmount\":25}",
                "{\"rushApproved\":true,\"rushFeeAmount\":25,\"expectedVersion\":0,\"status\":\"DONE\"}"
        )) {
            adminPatch(reference, body).andExpect(status().isBadRequest());
        }
        assertUnchanged(reference);
    }

    @Test
    void rejectsApprovalWithoutARushRequestAndReturnsNotFoundForMissingQuote() throws Exception {
        UUID reference = submitQuote(false);
        adminPatch(reference, decision(true, "0", 0)).andExpect(status().isConflict());
        adminPatch(UUID.randomUUID(), decision(true, "0", 0)).andExpect(status().isNotFound());
        assertUnchanged(reference);
    }

    @Test
    void preservesApprovedFeeWhenPriceOrVersionConflicts() throws Exception {
        UUID reference = submitQuote(true);
        adminPatch(reference, decision(true, "25", 2)).andExpect(status().isConflict());
        adminPatch(reference, decision(true, "25", 0)).andExpect(status().isOk());
        adminPatch(reference, decision(true, "30", 1)).andExpect(status().isConflict());
        adminPatch(reference, decision(false, "null", 0)).andExpect(status().isConflict());

        quote(reference)
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.rushFeeAmount").value(25.0));
        assertThat(audit(reference)).hasSize(1);
    }

    @Test
    void concurrentDifferentFeesProduceOneApprovalAndOneConflict() throws Exception {
        UUID reference = submitQuote(true);
        List<Integer> outcomes = race(reference, "15.00", "22.00");

        assertThat(outcomes).containsExactlyInAnyOrder(200, 409);
        var events = audit(reference);
        assertThat(events).hasSize(1);
        assertThat(events.getFirst().newFee())
                .isEqualByComparingTo(outcomes.getFirst() == 200 ? "15.00" : "22.00");
        quote(reference).andExpect(jsonPath("$.version").value(1));
    }

    @Test
    void concurrentIdenticalApprovalsDoNotDuplicateAudit() throws Exception {
        UUID reference = submitQuote(true);
        assertThat(race(reference, "15.00", "15.0")).containsExactly(200, 200);
        assertThat(audit(reference)).hasSize(1);
        quote(reference).andExpect(jsonPath("$.version").value(1));
    }

    @Test
    void rollsBackTheQuoteWhenTheAuditInsertFails() throws Exception {
        UUID reference = submitQuote(true);
        var input = new RushApprovalInput(true, new BigDecimal("25.50"), 0L);

        assertThatThrownBy(() -> rushApprovalService.apply(reference, input, ""))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("quote_rush_audit_actor_check");

        assertUnchanged(reference);
    }

    private UUID submitQuote(boolean rushRequested) throws Exception {
        String receipt = mockMvc.perform(post("/api/quote-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Test",
                                  "lastName": "Customer",
                                  "phone": "415-555-0100",
                                  "email": "rush-approval-test@example.com",
                                  "productId": 12,
                                  "quantity": 250,
                                  "fulfillmentMethod": "PICKUP",
                                  "rushRequested": %s,
                                  "rushNotes": "Need a Friday pickup."
                                }
                                """.formatted(rushRequested)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(JsonPath.read(receipt, "$.referenceNumber"));
    }

    private ResultActions quote(UUID reference) throws Exception {
        return mockMvc.perform(get(QUOTE_URL, reference).with(httpBasic("admin", TEST_PASSWORD)))
                .andExpect(status().isOk());
    }

    private ResultActions adminPatch(UUID reference, String body) throws Exception {
        return mockMvc.perform(rushRequest(reference, body)
                .with(csrf().asHeader())
                .with(httpBasic("admin", TEST_PASSWORD)));
    }

    private static MockHttpServletRequestBuilder rushRequest(UUID reference, String body) {
        return patch(RUSH_URL, reference).contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private static String decision(boolean approved, String fee, long version) {
        return "{\"rushApproved\":%s,\"rushFeeAmount\":%s,\"expectedVersion\":%d}"
                .formatted(approved, fee, version);
    }

    private void assertUnchanged(UUID reference) throws Exception {
        quote(reference)
                .andExpect(jsonPath("$.rushApproved").value(false))
                .andExpect(jsonPath("$.rushFeeAmount").doesNotExist())
                .andExpect(jsonPath("$.version").value(0))
                .andExpect(jsonPath("$.status").value("NEW"));
        assertThat(audit(reference)).isEmpty();
    }

    private List<Integer> race(UUID reference, String firstFee, String secondFee) throws Exception {
        var start = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var first = pool.submit(() -> racedPatch(reference, firstFee, start));
            var second = pool.submit(() -> racedPatch(reference, secondFee, start));
            start.countDown();
            return List.of(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS));
        }
    }

    private int racedPatch(UUID reference, String fee, CountDownLatch start) throws Exception {
        if (!start.await(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Concurrent test did not start.");
        }
        return adminPatch(reference, decision(true, fee, 0)).andReturn().getResponse().getStatus();
    }

    private List<Audit> audit(UUID reference) {
        return jdbcClient.sql("""
                        SELECT actor, old_version, new_version, old_rush_approved, new_rush_approved,
                               old_rush_fee_amount, new_rush_fee_amount, changed_at
                        FROM quote_rush_approval_audit
                        WHERE reference_number = :reference
                        ORDER BY new_version
                        """)
                .param("reference", reference)
                .query((row, number) -> new Audit(
                        row.getString("actor"), row.getLong("old_version"), row.getLong("new_version"),
                        row.getBoolean("old_rush_approved"), row.getBoolean("new_rush_approved"),
                        row.getBigDecimal("old_rush_fee_amount"), row.getBigDecimal("new_rush_fee_amount"),
                        row.getObject("changed_at", OffsetDateTime.class)
                ))
                .list();
    }

    private record Audit(String actor, long oldVersion, long newVersion, boolean oldApproved,
                         boolean newApproved, BigDecimal oldFee, BigDecimal newFee,
                         OffsetDateTime changedAt) {
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class DatabaseConfig {

        @Bean
        @ServiceConnection
        PostgreSQLContainer postgres() {
            return new PostgreSQLContainer("postgres:18.6")
                    .withDatabaseName("print_creations_rush_test");
        }
    }
}
