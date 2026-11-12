package com.printcreations.printcreations.quote;

import java.math.BigDecimal;
import java.util.UUID;

public record RushApprovalState(
        UUID referenceNumber,
        boolean rushRequested,
        boolean rushApproved,
        BigDecimal rushFeeAmount,
        long version
) {
}
