package com.printcreations.printcreations.quote;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record RushApprovalInput(
        @NotNull Boolean rushApproved,
        BigDecimal rushFeeAmount,
        @NotNull @PositiveOrZero Long expectedVersion
) {
    @JsonAnySetter
    public void rejectUnknownField(String name, Object value) {
        throw new IllegalArgumentException("Unexpected rush approval field: " + name);
    }
}
