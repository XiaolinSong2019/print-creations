package com.printcreations.printcreations.quote;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

import java.sql.Types;
import java.util.Map;
import java.util.UUID;

@Repository
public class QuoteRequestRepository {

    private final JdbcClient jdbcClient;
    private final JsonMapper jsonMapper;

    public QuoteRequestRepository(JdbcClient jdbcClient, JsonMapper jsonMapper) {
        this.jdbcClient = jdbcClient;
        this.jsonMapper = jsonMapper;
    }

    public QuoteRequestReceipt insert(QuoteRequestInput input) {
        boolean rushRequested = Boolean.TRUE.equals(input.rushRequested());
        Map<String, String> productOptions = input.productOptions() == null
                ? Map.of()
                : input.productOptions();

        return jdbcClient.sql("""
                INSERT INTO quote_requests (
                    first_name, last_name, phone, email, product_id, quantity,
                    fulfillment_method, company_name, needed_by, product_options,
                    special_instructions, rush_requested,
                    rush_requested_completion_date, rush_notes, marketing_opt_in
                ) VALUES (
                    :firstName, :lastName, :phone, :email, :productId, :quantity,
                    :fulfillmentMethod, :companyName, :neededBy, CAST(:productOptions AS jsonb),
                    :specialInstructions, :rushRequested,
                    :rushRequestedCompletionDate, :rushNotes, :marketingOptIn
                )
                RETURNING reference_number, status
                """)
                .param("firstName", input.firstName().strip())
                .param("lastName", input.lastName().strip())
                .param("phone", input.phone().strip())
                .param("email", input.email().strip())
                .param("productId", input.productId())
                .param("quantity", input.quantity())
                .param("fulfillmentMethod", input.fulfillmentMethod().name())
                .param("companyName", optionalText(input.companyName()))
                .param("neededBy", input.neededBy(), Types.DATE)
                .param("productOptions", jsonMapper.writeValueAsString(productOptions))
                .param("specialInstructions", optionalText(input.specialInstructions()))
                .param("rushRequested", rushRequested)
                .param(
                        "rushRequestedCompletionDate",
                        rushRequested ? input.rushRequestedCompletionDate() : null,
                        Types.DATE
                )
                .param("rushNotes", rushRequested ? optionalText(input.rushNotes()) : null)
                .param("marketingOptIn", Boolean.TRUE.equals(input.marketingOptIn()))
                .query((row, rowNumber) -> new QuoteRequestReceipt(
                        row.getObject("reference_number", UUID.class),
                        row.getString("status")
                ))
                .single();
    }

    private static String optionalText(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}