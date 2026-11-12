package com.printcreations.printcreations.quote;

import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.UUID;

@RestController
public class AdminRushApprovalController {

    private final RushApprovalService service;

    public AdminRushApprovalController(RushApprovalService service) {
        this.service = service;
    }

    @GetMapping("/api/admin/csrf")
    public ResponseEntity<CsrfResponse> csrf(CsrfToken token) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(new CsrfResponse(token.getHeaderName(), token.getToken()));
    }

    @PatchMapping(path = "/api/admin/quotes/{referenceNumber}/rush-approval",
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RushApprovalState> update(
            @PathVariable("referenceNumber") UUID referenceNumber,
            @Valid @RequestBody RushApprovalInput input,
            Principal principal
    ) {
        RushApprovalState state = service.apply(referenceNumber, input, principal.getName());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(state);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ProblemDetail handleRushError(ResponseStatusException exception) {
        return exception.getBody();
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
    public ProblemDetail handleInvalidBody() {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Provide a valid rushApproved, rushFeeAmount, and expectedVersion."
        );
    }

    public record CsrfResponse(String headerName, String token) {
    }
}
