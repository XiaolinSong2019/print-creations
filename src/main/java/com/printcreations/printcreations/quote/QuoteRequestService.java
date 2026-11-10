package com.printcreations.printcreations.quote;

import com.printcreations.printcreations.catalog.ProductCatalogService;
import org.springframework.stereotype.Service;

@Service
public class QuoteRequestService {

    private final ProductCatalogService catalogService;
    private final QuoteRequestRepository repository;

    public QuoteRequestService(
            ProductCatalogService catalogService,
            QuoteRequestRepository repository
    ) {
        this.catalogService = catalogService;
        this.repository = repository;
    }

    public QuoteRequestReceipt create(QuoteRequestInput input) {
        long productId = input.productId();
        boolean exists = catalogService.findAll().stream()
                .anyMatch(product -> product.id() == productId);

        if (!exists) {
            throw new UnknownProductException();
        }

        return repository.insert(input);
    }

    static final class UnknownProductException extends RuntimeException {
    }
}