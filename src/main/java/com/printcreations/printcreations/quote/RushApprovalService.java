package com.printcreations.printcreations.quote;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class RushApprovalService {

    private final RushApprovalRepository repository;

    public RushApprovalService(RushApprovalRepository repository) {
        this.repository = repository;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public RushApprovalState apply(UUID referenceNumber, RushApprovalInput input, String actor) {
        boolean approved = input.rushApproved();
        BigDecimal fee = validatedFee(approved, input.rushFeeAmount());
        long expectedVersion = input.expectedVersion();
        RushApprovalState before = requireQuote(referenceNumber);

        if (approved && !before.rushRequested()) {
            throw conflict("The customer did not request rush service.");
        }
        if (expectedVersion > before.version()) {
            throw versionConflict();
        }
        if (matches(before, approved, fee)) {
            return before;
        }
        if (approved && before.rushApproved()) {
            throw conflict("Rush was already approved at a different fee.");
        }
        if (expectedVersion != before.version()) {
            throw versionConflict();
        }

        var changed = repository.updateIfVersionMatches(referenceNumber, expectedVersion, approved, fee);
        if (changed.isEmpty()) {
            // Re-read after the concurrent transaction to recognize an identical retry.
            RushApprovalState latest = requireQuote(referenceNumber);
            if (matches(latest, approved, fee)) {
                return latest;
            }
            throw versionConflict();
        }

        RushApprovalState after = changed.orElseThrow();
        repository.insertAudit(actor, before, after);
        return after;
    }

    private RushApprovalState requireQuote(UUID referenceNumber) {
        return repository.find(referenceNumber)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quote not found."));
    }

    private static BigDecimal validatedFee(boolean approved, BigDecimal fee) {
        if (!approved) {
            if (fee != null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A cleared rush decision must have a null fee.");
            }
            return null;
        }

        if (fee == null || fee.signum() < 0 || fee.scale() > 2
                || (long) fee.precision() - fee.scale() > 10) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Approval requires a nonnegative USD fee with up to ten integer digits and two decimal places."
            );
        }
        return fee.setScale(2);
    }

    private static boolean matches(RushApprovalState state, boolean approved, BigDecimal fee) {
        if (state.rushApproved() != approved) {
            return false;
        }
        BigDecimal storedFee = state.rushFeeAmount();
        return storedFee == null ? fee == null : fee != null && storedFee.compareTo(fee) == 0;
    }

    private static ResponseStatusException conflict(String detail) {
        return new ResponseStatusException(HttpStatus.CONFLICT, detail);
    }

    private static ResponseStatusException versionConflict() {
        return conflict("The quote changed. Reload the current quote before changing the rush decision.");
    }
}
