package com.printcreations.printcreations.quote;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class AdminQuoteController {

    private final AdminQuoteRepository repository;

    public AdminQuoteController(AdminQuoteRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/api/admin/quotes/{referenceNumber}")
    public ResponseEntity<AdminQuoteDetails> getQuote(
            @PathVariable("referenceNumber") UUID referenceNumber
    ) {
        return repository.findByReference(referenceNumber)
                .map(quote -> ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(quote))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
