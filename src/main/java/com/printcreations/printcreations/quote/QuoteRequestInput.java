package com.printcreations.printcreations.quote;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.Map;

public record QuoteRequestInput(
        @NotBlank(message = "Enter your first name.")
        @Size(max = 100)
        String firstName,

        @NotBlank(message = "Enter your last name.")
        @Size(max = 100)
        String lastName,

        @NotBlank(message = "Enter a phone number.")
        @Size(max = 40)
        @Pattern(
                regexp = "^(?=(?:\\D*\\d){7,15}\\D*$)\\+?[0-9(). -]+$",
                message = "Enter a valid phone number."
        )
        String phone,

        @NotBlank(message = "Enter your email address.")
        @Email(message = "Enter a valid email address.")
        @Size(max = 254)
        String email,

        @NotNull(message = "Select a product.")
        @Positive(message = "Select a product from the catalog.")
        Long productId,

        @NotNull(message = "Enter a quantity.")
        @Positive(message = "Quantity must be a positive integer.")
        Integer quantity,

        @NotNull(message = "Choose pickup, mailing, or delivery.")
        FulfillmentMethod fulfillmentMethod,

        @Size(max = 150) String companyName,
        LocalDate neededBy,
        @Size(max = 20)
        Map<@NotBlank @Size(max = 80) String, @NotBlank @Size(max = 250) String> productOptions,
        @Size(max = 2000) String specialInstructions,
        Boolean rushRequested,
        LocalDate rushRequestedCompletionDate,
        @Size(max = 1000) String rushNotes,
        Boolean marketingOptIn
) {
    public enum FulfillmentMethod {
        PICKUP,
        MAILING,
        DELIVERY
    }
}