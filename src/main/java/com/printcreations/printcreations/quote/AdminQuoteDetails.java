package com.printcreations.printcreations.quote;

import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AdminQuoteDetails(
        UUID referenceNumber,
        String status,
        String firstName,
        String lastName,
        String companyName,
        String phone,
        String email,
        long productId,
        int quantity,
        String fulfillmentMethod,
        LocalDate neededBy,
        JsonNode productOptions,
        String specialInstructions,
        boolean rushRequested,
        LocalDate rushRequestedCompletionDate,
        String rushNotes,
        boolean rushApproved,
        BigDecimal rushFeeAmount,
        boolean marketingOptIn,
        OffsetDateTime createdAt,
        long version
) {
}