package com.printcreations.printcreations.quote;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public class AdminQuoteRepository {

    private final JdbcClient jdbcClient;
    private final JsonMapper jsonMapper;

    public AdminQuoteRepository(JdbcClient jdbcClient, JsonMapper jsonMapper) {
        this.jdbcClient = jdbcClient;
        this.jsonMapper = jsonMapper;
    }

    public Optional<AdminQuoteDetails> findByReference(UUID referenceNumber) {
        return jdbcClient.sql("""
                        SELECT reference_number, status, first_name, last_name,
                               company_name, phone, email, product_id, quantity,
                               fulfillment_method, needed_by, product_options,
                               special_instructions, rush_requested,
                               rush_requested_completion_date, rush_notes,
                               rush_approved, rush_fee_amount, marketing_opt_in,
                               created_at, version
                        FROM quote_requests
                        WHERE reference_number = :referenceNumber
                        """)
                .param("referenceNumber", referenceNumber)
                .query((row, rowNumber) -> new AdminQuoteDetails(
                        row.getObject("reference_number", UUID.class),
                        row.getString("status"),
                        row.getString("first_name"),
                        row.getString("last_name"),
                        row.getString("company_name"),
                        row.getString("phone"),
                        row.getString("email"),
                        row.getLong("product_id"),
                        row.getInt("quantity"),
                        row.getString("fulfillment_method"),
                        row.getObject("needed_by", LocalDate.class),
                        jsonMapper.readTree(row.getString("product_options")),
                        row.getString("special_instructions"),
                        row.getBoolean("rush_requested"),
                        row.getObject("rush_requested_completion_date", LocalDate.class),
                        row.getString("rush_notes"),
                        row.getBoolean("rush_approved"),
                        row.getBigDecimal("rush_fee_amount"),
                        row.getBoolean("marketing_opt_in"),
                        row.getObject("created_at", OffsetDateTime.class),
                        row.getLong("version")
                ))
                .optional();
    }
}