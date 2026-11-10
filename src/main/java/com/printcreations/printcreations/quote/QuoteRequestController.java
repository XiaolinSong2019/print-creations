package com.printcreations.printcreations.quote;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/quote-requests")
public class QuoteRequestController {

    private final QuoteRequestService service;

    public QuoteRequestController(QuoteRequestService service) {
        this.service = service;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public QuoteRequestReceipt create(@Valid @RequestBody QuoteRequestInput input) {
        return service.create(input);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleInvalidFields(MethodArgumentNotValidException exception) {
        Map<String, String> errors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error ->
                errors.putIfAbsent(error.getField(), error.getDefaultMessage())
        );
        return badRequest("Check the marked fields and try again.", errors);
    }

    @ExceptionHandler(QuoteRequestService.UnknownProductException.class)
    ProblemDetail handleUnknownProduct() {
        return badRequest(
                "Select a product from the catalog.",
                Map.of("productId", "Select a product from the catalog.")
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail handleUnreadableBody() {
        return badRequest(
                "Check the JSON body. Use whole numbers for product ID and quantity, "
                        + "PICKUP, MAILING, or DELIVERY for fulfillment, and YYYY-MM-DD for dates.",
                Map.of()
        );
    }

    private static ProblemDetail badRequest(String detail, Map<String, String> errors) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problem.setTitle("Invalid quote request");
        if (!errors.isEmpty()) {
            problem.setProperty("errors", errors);
        }
        return problem;
    }
}